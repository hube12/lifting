# Cracking Minecraft seed with structure only in an efficient manner

Use Lifting to split the 48 bit range into a 18,19 or 20 bit range and their complement 30,29 or 28 bit range.

First collect all valid seed %2,4 or 8 in the low range then iterate through the high range with the low bit stream.

Efficient because now you only have at most 2^32ish comparison.


Works with decorators till 1.17 and works with structure in all version of minecraft
