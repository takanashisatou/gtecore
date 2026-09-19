"""Generate a six-command-block thermostat for testing an existing T1 unit.

This is an administrator debugging tool, not a survival redstone circuit.
It reads machine NBT and toggles a real adjacent redstone block; it never changes
temperature, efficiency, power, recipes, or maintenance NBT.
"""
import argparse
import json
import re
import zipfile
from pathlib import Path


def coordinates(values):
    return " ".join(map(str, values))


def generate(controller, output, console, objective):
    if not re.fullmatch(r"[A-Za-z0-9_.+-]{1,16}", objective):
        raise ValueError("Use 1-16 letters/digits/_.+- for the scoreboard objective")
    positions = [(console[0] + index, console[1], console[2]) for index in range(6)]
    if tuple(controller) == tuple(output) or tuple(controller) in positions or tuple(output) in positions:
        raise ValueError("Controller, redstone output, and console positions must not overlap")
    c, r = coordinates(controller), coordinates(output)
    # Machine custom data is serialized into the block entity's root tag.
    root = "gtePurificationThermal"
    valid = f"if score #temp_ok {objective} matches 1 if score #target_ok {objective} matches 1"
    off = f"if block {r} minecraft:redstone_block run setblock {r} minecraft:air"
    commands = [
        f"execute store success score #temp_ok {objective} store result score #temp {objective} run data get block {c} {root}.temperature 1",
        f"execute store success score #target_ok {objective} store result score #target {objective} run data get block {c} {root}.center 10",
        f"execute {valid} if score #temp {objective} < #target {objective} if block {r} minecraft:air run setblock {r} minecraft:redstone_block",
        f"execute {valid} if score #temp {objective} >= #target {objective} {off}",
        f"execute unless score #temp_ok {objective} matches 1 {off}",
        f"execute unless score #target_ok {objective} matches 1 {off}",
    ]
    lines = [
        "# Administrator-only T1 thermostat. Use an existing, supplied, formed T1.",
        "# Output must be an empty block immediately adjacent to the thermal hatch.",
        "# Console requires six empty blocks along +X, in the SAME dimension as the T1.",
        "# Run these commands in order; 'keep' avoids replacing occupied console blocks.",
        f"scoreboard objectives add {objective} dummy",
    ]
    # Place the powered repeating block last, after all chain blocks exist.
    for index in [1, 2, 3, 4, 5, 0]:
        block = "repeating_command_block" if index == 0 else "chain_command_block"
        nbt = "{auto:1b,TrackOutput:0b,Command:" + json.dumps(commands[index], ensure_ascii=True) + "}"
        lines.append(f"setblock {coordinates(positions[index])} minecraft:{block}[facing=east,conditional=false]{nbt} keep")
    lines += ["", "# Stop the controller before manually testing a fault:",
              f"# data merge block {coordinates(positions[0])} {{auto:0b}}",
              f"# Heat: execute if block {r} minecraft:air run setblock {r} minecraft:redstone_block",
              f"# Cool: execute {off}",
              f"# Resume: data merge block {coordinates(positions[0])} {{auto:1b}}",
              "", "# Debug: the two scores use tenths of a degree Celsius.",
              f"# scoreboard players get #temp {objective}",
              f"# scoreboard players get #target {objective}",
              f"# data get block {c} {root}",
              "", "# Full removal (stop first; remove only these command blocks):"]
    for index, position in enumerate(positions):
        block = "repeating_command_block" if index == 0 else "chain_command_block"
        at = coordinates(position)
        lines.append(f"# execute if block {at} minecraft:{block} run setblock {at} minecraft:air")
    lines += [f"# execute {off}", f"# scoreboard objectives remove {objective}"]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--controller", nargs=3, type=int, required=True, metavar=("X", "Y", "Z"))
    parser.add_argument("--output", nargs=3, type=int, required=True, metavar=("X", "Y", "Z"))
    parser.add_argument("--console", nargs=3, type=int, required=True, metavar=("X", "Y", "Z"))
    parser.add_argument("--objective", default="gteTherm")
    parser.add_argument("--file", type=Path)
    parser.add_argument("--pack", type=Path, help="Write an installable Minecraft 1.20.1 datapack ZIP")
    args = parser.parse_args()
    result = generate(args.controller, args.output, args.console, args.objective)
    if args.file:
        args.file.parent.mkdir(parents=True, exist_ok=True)
        args.file.write_text(result, encoding="utf-8")
        print(args.file.resolve())
    if args.pack:
        args.pack.parent.mkdir(parents=True, exist_ok=True)
        console = coordinates(args.console)
        output = coordinates(args.output)
        disable = f"execute if block {console} minecraft:repeating_command_block run data merge block {console} {{auto:0b}}\n"
        cool = f"execute if block {output} minecraft:redstone_block run setblock {output} minecraft:air\n"
        heat = f"execute if block {output} minecraft:air run setblock {output} minecraft:redstone_block\n"
        remove = disable + cool
        for index in range(6):
            at = coordinates((args.console[0] + index, args.console[1], args.console[2]))
            block = "repeating_command_block" if index == 0 else "chain_command_block"
            remove += f"execute if block {at} minecraft:{block} run setblock {at} minecraft:air\n"
        remove += f"scoreboard objectives remove {args.objective}\n"
        functions = {
            "install": result,
            "stop": disable + cool,
            "heat": disable + heat,
            "cool": disable + cool,
            "resume": f"execute if block {console} minecraft:repeating_command_block run data merge block {console} {{auto:1b}}\n",
            "remove": remove,
        }
        with zipfile.ZipFile(args.pack, "w", zipfile.ZIP_DEFLATED) as archive:
            archive.writestr("pack.mcmeta", json.dumps({"pack": {
                "pack_format": 15, "description": "GTE T1 administrator thermal test (real redstone input)"
            }}))
            for name, commands in functions.items():
                archive.writestr(f"data/gte_thermal_test/functions/{name}.mcfunction", commands)
        print(args.pack.resolve())
    if not args.file and not args.pack:
        print(result, end="")


if __name__ == "__main__":
    main()
