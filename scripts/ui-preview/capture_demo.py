#!/usr/bin/env python3
"""Install a trusted Vendora APK, create disposable demo data through the UI, and capture screens."""
import argparse
import os
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "dev.vendora.android"
ACTIVITY = "dev.vendora.android.MainActivity"


def adb(*args, check=True, capture=False):
    result = subprocess.run(
        ["adb", *map(str, args)],
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )
    return result.stdout.strip() if capture else ""


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml", check=False, capture=True)
    xml = adb("exec-out", "cat", "/sdcard/window.xml", capture=True)
    try:
        return ET.fromstring(xml)
    except ET.ParseError as exc:
        raise RuntimeError(f"Could not read the current Android UI hierarchy: {exc}") from exc


def find_node(needle, exact=False, timeout=20):
    deadline = time.time() + timeout
    needle = needle.casefold()
    last = []
    while time.time() < deadline:
        root = hierarchy()
        matches = []
        for node in root.iter("node"):
            text = (node.attrib.get("text") or "").strip()
            desc = (node.attrib.get("content-desc") or "").strip()
            values = [value for value in (text, desc) if value]
            if any((value.casefold() == needle if exact else needle in value.casefold()) for value in values):
                bounds = node.attrib.get("bounds", "")
                if bounds:
                    matches.append((node, bounds, values))
        if matches:
            # Prefer the exact label, then the smallest matching target.
            def target_area(item):
                left_top, right_bottom = item[1][1:-1].split("][")
                left, top = map(int, left_top.split(","))
                right, bottom = map(int, right_bottom.split(","))
                return max(0, right - left) * max(0, bottom - top)

            matches.sort(key=lambda item: (
                0 if any(v.casefold() == needle for v in item[2]) else 1,
                target_area(item),
            ))
            return matches[0]
        last = [n.attrib for n in root.iter("node") if n.attrib.get("text") or n.attrib.get("content-desc")]
        time.sleep(0.6)
    visible = [x.get("text") or x.get("content-desc") for x in last[:35]]
    raise RuntimeError(f"Timed out waiting for UI text {needle!r}; visible labels: {visible}")


def tap_text(needle, exact=False, timeout=20):
    _, bounds, _ = find_node(needle, exact=exact, timeout=timeout)
    left_top, right_bottom = bounds[1:-1].split("][")
    left, top = map(int, left_top.split(","))
    right, bottom = map(int, right_bottom.split(","))
    adb("shell", "input", "tap", (left + right) // 2, (top + bottom) // 2)
    time.sleep(0.7)


def type_text(value):
    # Android input text uses %s for spaces. Demo strings intentionally stay ASCII.
    adb("shell", "input", "text", str(value).replace(" ", "%s"))
    time.sleep(0.25)


def fill_field(label, value):
    # Dialog fields may be below the fold on a phone-sized emulator.
    for attempt in range(4):
        try:
            tap_text(label, timeout=2)
            type_text(value)
            return
        except RuntimeError:
            if attempt == 3:
                raise
            adb("shell", "input", "swipe", 540, 1100, 540, 480, 350)
            time.sleep(0.4)


def hide_keyboard():
    # The back key dismisses the IME first when it is open.
    adb("shell", "input", "keyevent", "4", check=False)
    time.sleep(0.3)


def screenshot(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as output:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], check=True, stdout=output)
    if path.stat().st_size < 10_000:
        raise RuntimeError(f"Screenshot seems unexpectedly small: {path}")
    print(f"Captured {path} ({path.stat().st_size} bytes)")


def add_product(name, sku, quantity, purchase, selling):
    tap_text("Products", timeout=15)
    tap_text("+", exact=True, timeout=15)
    find_node("Product name", timeout=10)
    fill_field("Product name", name)
    fill_field("SKU (optional)", sku)
    fill_field("Stock quantity", quantity)
    fill_field("Purchase price", purchase)
    fill_field("Selling price", selling)
    hide_keyboard()
    tap_text("Save product", timeout=10)
    find_node(name, timeout=15)
    time.sleep(0.8)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    apk = Path(args.apk)
    output = Path(args.output)
    if not apk.is_file():
        raise FileNotFoundError(f"APK artifact is missing: {apk}")

    # The emulator is ephemeral. No user device or production data is accessed.
    adb("install", "-r", apk)
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/{ACTIVITY}")
    find_node("Create shop", timeout=35)
    fill_field("Shop name", "Vendora Demo Mart")
    hide_keyboard()
    tap_text("Create shop", timeout=10)
    find_node("Overview", timeout=30)
    time.sleep(2)

    add_product("Rice 5kg", "DEMO-RICE", "12", "480", "560")
    add_product("Tea Pack", "DEMO-TEA", "8", "120", "165")
    add_product("Soap Bar", "DEMO-SOAP", "3", "35", "50")

    tap_text("Overview", timeout=15)
    time.sleep(1.5)
    screenshot(output / "home.png")

    tap_text("Products", timeout=15)
    time.sleep(1.0)
    screenshot(output / "products.png")

    tap_text("Selling", timeout=15)
    time.sleep(1.0)
    screenshot(output / "selling.png")

    tap_text("Insights", timeout=15)
    time.sleep(1.0)
    screenshot(output / "insights.png")

    # Ensure the published preview is complete, rather than silently publishing partial captures.
    expected = ["home.png", "products.png", "selling.png", "insights.png"]
    missing = [name for name in expected if not (output / name).is_file()]
    if missing:
        raise RuntimeError(f"Missing screenshots: {missing}")
    print("Demo data created only inside the disposable emulator; screenshots complete.")


if __name__ == "__main__":
    main()
