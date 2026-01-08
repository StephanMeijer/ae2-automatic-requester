#!/usr/bin/env python3
"""Generate AE2-style textures for the Autorequester block with animated LEDs."""

from PIL import Image, ImageDraw
import json

# AE2-inspired color palette
COLORS = {
    # Frame and body
    'frame_dark': (32, 32, 35),
    'frame_mid': (45, 45, 50),
    'frame_light': (58, 58, 65),
    'body_dark': (38, 40, 45),
    'body_mid': (48, 52, 58),
    'body_light': (62, 66, 72),

    # Accents
    'circuit_dark': (55, 60, 68),
    'circuit_light': (70, 78, 88),

    # Certus/tech glow
    'certus_dark': (60, 140, 145),
    'certus_mid': (80, 175, 180),
    'certus_light': (120, 210, 215),

    # Screen/display
    'screen_dark': (25, 35, 45),
    'screen_mid': (35, 50, 65),
    'screen_border': (50, 65, 80),

    # Status lights - bright (on) state
    'light_off': (40, 42, 48),
    'light_idle_bright': (80, 180, 220),      # Cyan
    'light_active_bright': (100, 220, 120),   # Green
    'light_error_bright': (220, 80, 80),      # Red
    'light_warning_bright': (220, 180, 60),   # Yellow/orange

    # Status lights - dim state (for blinking)
    'light_idle_dim': (50, 100, 130),
    'light_active_dim': (60, 130, 80),
    'light_error_dim': (130, 50, 50),
    'light_warning_dim': (130, 110, 40),

    # Glow effects (lighter versions)
    'glow_idle': (140, 210, 240),
    'glow_active': (160, 240, 180),
    'glow_error': (250, 140, 140),
    'glow_warning': (250, 220, 120),
}

def create_base_frame(img: Image.Image) -> None:
    """Draw the standard AE2-style frame border."""
    draw = ImageDraw.Draw(img)

    # Outer frame (darkest)
    draw.rectangle([0, 0, 15, 15], fill=COLORS['frame_dark'])

    # Inner frame
    draw.rectangle([1, 1, 14, 14], fill=COLORS['frame_mid'])

    # Body area
    draw.rectangle([2, 2, 13, 13], fill=COLORS['body_dark'])

    # Corner accents (AE2 style small dots)
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        img.putpixel((x, y), COLORS['frame_light'])


def create_side_texture() -> Image.Image:
    """Create the side texture with circuit-like patterns."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    create_base_frame(img)
    draw = ImageDraw.Draw(img)

    # Fill body with gradient-like pattern
    for y in range(3, 13):
        for x in range(3, 13):
            if (x + y) % 4 == 0:
                img.putpixel((x, y), COLORS['body_mid'])
            elif (x + y) % 4 == 2:
                img.putpixel((x, y), COLORS['body_light'])

    # Horizontal circuit lines
    draw.line([(3, 5), (12, 5)], fill=COLORS['circuit_dark'])
    draw.line([(3, 10), (12, 10)], fill=COLORS['circuit_dark'])

    # Vertical circuit segments
    draw.line([(5, 3), (5, 5)], fill=COLORS['circuit_dark'])
    draw.line([(10, 10), (10, 12)], fill=COLORS['circuit_dark'])

    # Small certus accent dots
    img.putpixel((5, 5), COLORS['certus_dark'])
    img.putpixel((10, 10), COLORS['certus_dark'])

    # Add subtle panel lines
    draw.line([(2, 7), (13, 7)], fill=COLORS['frame_mid'])
    draw.line([(2, 8), (13, 8)], fill=COLORS['body_light'])

    return img


def create_top_texture() -> Image.Image:
    """Create the top texture with vent/grid pattern."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    create_base_frame(img)

    # Vent grid pattern
    for y in range(3, 13, 2):
        for x in range(3, 13, 2):
            img.putpixel((x, y), COLORS['body_mid'])
            img.putpixel((x+1, y), COLORS['body_light'])
            img.putpixel((x, y+1), COLORS['body_light'])
            img.putpixel((x+1, y+1), COLORS['frame_mid'])

    # Center certus crystal accent
    for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        img.putpixel((x, y), COLORS['certus_dark'])
    img.putpixel((7, 7), COLORS['certus_mid'])

    return img


def create_bottom_texture() -> Image.Image:
    """Create the bottom texture with connection points."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    create_base_frame(img)
    draw = ImageDraw.Draw(img)

    # Grid pattern for mounting
    for y in range(3, 13):
        for x in range(3, 13):
            if (x + y) % 2 == 0:
                img.putpixel((x, y), COLORS['body_mid'])

    # Connection point indicators (corners)
    for cx, cy in [(4, 4), (11, 4), (4, 11), (11, 11)]:
        draw.rectangle([cx-1, cy-1, cx+1, cy+1], fill=COLORS['frame_light'])
        img.putpixel((cx, cy), COLORS['certus_dark'])

    # Center mounting area
    draw.rectangle([6, 6, 9, 9], fill=COLORS['frame_mid'])
    draw.rectangle([7, 7, 8, 8], fill=COLORS['body_dark'])

    return img


def create_front_base(img: Image.Image) -> None:
    """Create the front texture base (without status light)."""
    create_base_frame(img)
    draw = ImageDraw.Draw(img)

    # Screen/display area (upper portion)
    draw.rectangle([3, 3, 12, 9], fill=COLORS['screen_border'])
    draw.rectangle([4, 4, 11, 8], fill=COLORS['screen_dark'])

    # Screen content lines (like a display showing rules)
    draw.line([(5, 5), (10, 5)], fill=COLORS['screen_mid'])
    draw.line([(5, 6), (9, 6)], fill=COLORS['screen_mid'])
    draw.line([(5, 7), (10, 7)], fill=COLORS['screen_mid'])

    # Certus accent on screen border
    img.putpixel((3, 3), COLORS['certus_dark'])
    img.putpixel((12, 3), COLORS['certus_dark'])

    # Status light housing (lower portion)
    draw.rectangle([6, 11, 9, 13], fill=COLORS['frame_mid'])

    # Small indicator dots on sides
    img.putpixel((3, 11), COLORS['circuit_light'])
    img.putpixel((12, 11), COLORS['circuit_light'])


def add_status_light(img: Image.Image, light_color: tuple, glow_color: tuple = None) -> None:
    """Add a status light to the front texture."""
    draw = ImageDraw.Draw(img)

    # Status light itself
    draw.rectangle([7, 11, 8, 12], fill=light_color)

    # Add glow effect for active states
    if glow_color:
        for gx, gy in [(6, 11), (9, 11), (6, 12), (9, 12), (7, 13), (8, 13)]:
            if 0 <= gx < 16 and 0 <= gy < 16:
                existing = img.getpixel((gx, gy))
                blended = tuple(min(255, (e + g) // 2) for e, g in zip(existing[:3], glow_color))
                img.putpixel((gx, gy), blended + (255,))


def create_front_texture(status: str = 'off', bright: bool = True) -> Image.Image:
    """Create the front texture with screen and status light."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    create_front_base(img)

    if status == 'off':
        add_status_light(img, COLORS['light_off'], None)
    else:
        suffix = '_bright' if bright else '_dim'
        light_color = COLORS.get(f'light_{status}{suffix}', COLORS['light_off'])
        glow_color = COLORS.get(f'glow_{status}') if bright else None
        add_status_light(img, light_color, glow_color)

    return img


def create_animated_front_texture(status: str) -> Image.Image:
    """Create an animated texture strip for blinking LED (4 frames)."""
    # Animation pattern: bright -> bright -> dim -> dim (creates a blink)
    frames = []

    if status == 'off':
        # No animation for off state
        return create_front_texture('off')

    # Create 4 frames for smooth blinking
    # Frame 0-1: bright, Frame 2-3: dim
    frames.append(create_front_texture(status, bright=True))
    frames.append(create_front_texture(status, bright=True))
    frames.append(create_front_texture(status, bright=False))
    frames.append(create_front_texture(status, bright=False))

    # Create vertical strip (frames stacked vertically)
    strip = Image.new('RGBA', (16, 16 * len(frames)), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * 16))

    return strip


def create_mcmeta(frametime: int = 10) -> dict:
    """Create mcmeta content for animated texture."""
    return {
        "animation": {
            "frametime": frametime,  # Ticks per frame (20 ticks = 1 second)
            "interpolate": False
        }
    }


def main():
    """Generate all textures."""
    import os

    output_dir = 'src/main/resources/assets/ae2_autorequester/textures/block'
    os.makedirs(output_dir, exist_ok=True)

    # Generate base textures (non-animated)
    textures = {
        'autorequester_side.png': create_side_texture(),
        'autorequester_top.png': create_top_texture(),
        'autorequester_bottom.png': create_bottom_texture(),
    }

    # Generate front textures - OFF is static, others are animated
    textures['autorequester_front.png'] = create_front_texture('off')
    textures['autorequester_front_off.png'] = create_front_texture('off')

    # Save static textures
    for name, img in textures.items():
        path = os.path.join(output_dir, name)
        img.save(path)
        print(f'Created: {path}')

    # Generate animated front textures with mcmeta
    animated_statuses = ['idle', 'active', 'error', 'warning']

    for status in animated_statuses:
        # Create animated texture strip
        strip = create_animated_front_texture(status)
        name = f'autorequester_front_{status}.png'
        path = os.path.join(output_dir, name)
        strip.save(path)
        print(f'Created: {path} (animated, {strip.height // 16} frames)')

        # Create mcmeta file
        # Different blink speeds for different statuses
        if status == 'active':
            frametime = 8   # Fast blink when crafting
        elif status == 'error':
            frametime = 5   # Urgent fast blink for errors
        elif status == 'warning':
            frametime = 12  # Medium blink for warnings
        else:  # idle
            frametime = 20  # Slow pulse for idle

        mcmeta = create_mcmeta(frametime)
        mcmeta_path = path + '.mcmeta'
        with open(mcmeta_path, 'w') as f:
            json.dump(mcmeta, f, indent=2)
        print(f'Created: {mcmeta_path}')

    print(f'\nGenerated {len(textures) + len(animated_statuses)} textures with {len(animated_statuses)} animations!')


if __name__ == '__main__':
    main()
