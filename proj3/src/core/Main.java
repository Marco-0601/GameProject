package core;
import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Main {
    private static String userInput = "";
    private static World mainWorld;
    private static Character player;
    private static boolean loaded = false;
    private static TETile wall = Tileset.WALL;
    private static TETile floor = Tileset.FLOOR;
    private static TETile nothing = Tileset.NOTHING;
    private static int darkMode = 0;
    private static final int WORLD_WIDTH = 90;
    private static final int WORLD_HEIGHT = 30;
    private static final int RENDER_RATE = 10;

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WORLD_WIDTH, WORLD_HEIGHT);

        showMenu();
        // Initial Menu, Wait for key press
        while (true) {
            if (StdDraw.isKeyPressed('N') || StdDraw.isKeyPressed('n')) {
                seedEnterer();
                break;
            }
            if (StdDraw.isKeyPressed('Q') || StdDraw.isKeyPressed('q')) {
                System.exit(0);
            }
            if (StdDraw.isKeyPressed('L') || StdDraw.isKeyPressed('l')) {
                loadGame();
                break;
            }
            if (StdDraw.isKeyPressed('T') || StdDraw.isKeyPressed('t')) {
                chooseTheme();
                showMenu();
            }
        }

        if (!loaded) {
            Coordinate spawnPoint = mainWorld.getRandomFloorTile();
            player = new Character(spawnPoint, mainWorld.world, floor);
        }
        Set<Integer> pressedKeys = new HashSet<>();
        Set<Integer> prevPressedKeys = new HashSet<>();
        boolean colonPressed = false;

        // Main game loop
        while (true) {
            //check for :q/:Q to quit the game
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                // Detect colon key
                if (key == ':') {
                    colonPressed = true;
                }
                // Detect Q or q after colon key
                if (colonPressed && (key == 'Q' || key == 'q')) {
                    colonPressed = false; // Reset state after function is executed
                    loaded = true;
                    saveGame();
                    System.exit(0);
                }
                if (key != ':' && key != 'Q' && key != 'q') {
                    colonPressed = false; // Reset state after function is executed
                }
                if (key == 'e' || key == 'E') {
                    darkMode = (darkMode + 1) % 3; // Cycle through 0 -> 1 -> 2 -> 0
                }
            }
            // Update pressed keys
            updatePressedKeys(pressedKeys);
            // Handle movement
            for (Integer key : pressedKeys) {
                if (!prevPressedKeys.contains(key)) { // Move only if key is newly pressed
                    movePlayer(Set.of(key), player);
                    break; // Process only one movement per frame
                }
            }
            prevPressedKeys.clear();
            prevPressedKeys.addAll(pressedKeys);
            TETile[][] worldFrame;
            // Render the updated world with the character
            if (darkMode == 0) {
                worldFrame = mainWorld.getWorldWithCharacter(player);
            } else if (darkMode == 1) {
                worldFrame = mainWorld.getWorldDark(player);
            } else {
                worldFrame = mainWorld.getOmniWorldDark(player);
            }
            ter.resetFont();
            ter.renderFrame(worldFrame);
            StdDraw.pause(RENDER_RATE); // Adjust pause for smoother movement
        }
    }

    // Method to update the set of pressed keys
    private static void updatePressedKeys(Set<Integer> pressedKeys) {
        pressedKeys.clear(); // Clear the current set of pressed keys

        if (StdDraw.isKeyPressed(87)) { // 'W'
            pressedKeys.add(87);
        }
        if (StdDraw.isKeyPressed(83)) { // 'S'
            pressedKeys.add(83);
        }
        if (StdDraw.isKeyPressed(65)) { // 'A'
            pressedKeys.add(65);
        }
        if (StdDraw.isKeyPressed(68)) { // 'D'
            pressedKeys.add(68);
        }
    }

    private static void generateWorld(Long seed) {
        mainWorld = new World(seed, wall, floor, nothing);
    }

    //first-menu
    private static void showMenu() {
        StdDraw.setCanvasSize(900, 300);
        StdDraw.setXscale(0, 90);
        StdDraw.setYscale(0, 30);

        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        StdDraw.text(45, 24, "CS61B: BYOW");
        StdDraw.text(45, 18, "(N) New Game");
        StdDraw.text(45, 14, "(L) Load Game");
        StdDraw.text(45, 10, "(T) Choose Theme");
        StdDraw.text(45, 6, "(Q) Quit Game");

        // Display the black screen
        StdDraw.show();
    }

    //code that runs the seed entering idea
    private static void seedEnterer() {

        userInput = ""; // Initialize empty input
        redrawNumberInputScreen(userInput);

        while (true) {
            boolean updated = false; // Track if the screen needs updating

            // Check for number keys
            for (int i = 0; i <= 9; i++) {
                if (StdDraw.isKeyPressed('0' + i)) {
                    userInput += i; // Append the pressed number to the input string
                    waitForKeyRelease('0' + i); // Ensure one key press is registered at a time
                    updated = true; // Mark that input has been updated
                }
            }
            if (StdDraw.isKeyPressed('S') || StdDraw.isKeyPressed('s')) {
                generateWorld(Long.parseLong(userInput));
                break;
            }
            // Redraw only if the input was updated
            if (updated) {
                redrawNumberInputScreen(userInput);
            }
        }
    }

    // Function to redraw the number input screen
    private static void redrawNumberInputScreen(String userInput) {

        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        StdDraw.text(45, 25, "CS61B: BYOW");
        StdDraw.text(45, 20, "Enter seed followed by S");
        StdDraw.text(45, 15, userInput); // Show the user's current input
        StdDraw.show();
    }

    // Helper function to wait for a key release
    private static void waitForKeyRelease(int key) {
        while (StdDraw.isKeyPressed(key)) {
            // Do nothing, wait for key release
        }
    }

    // Method to move the player based on the set of pressed keys
    private static void movePlayer(Set<Integer> pressedKeys, Character player) {
        int dx = 0; // Horizontal movement (-1 = left, +1 = right)
        int dy = 0; // Vertical movement (-1 = down, +1 = up)

        if (pressedKeys.contains(87)) { // 'W' key
            dy += 1; // Move up
        }
        if (pressedKeys.contains(83)) { // 'S' key
            dy -= 1; // Move down
        }
        if (pressedKeys.contains(65)) { // 'A' key
            dx -= 1; // Move left
        }
        if (pressedKeys.contains(68)) { // 'D' key
            dx += 1; // Move right
        }

        // Apply the movement to the player
        player.move(dx, dy);
    }

    //Load Game function
    private static void loadGame() {

        File file = new File("proj3/src/core/save.txt");
        //exits with no game saved
        if (!file.exists()) {
            System.out.println("No saved game found.");
            System.exit(0); //exit the game
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("proj3/src/core/save.txt"))) {
            userInput = reader.readLine();
            Long seed = Long.parseLong(userInput); // Read the first line for seed
            String playerPosition = reader.readLine(); // Read the second line for x,y coordinates
            String[] coordinates = playerPosition.split(",");
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);

            loaded = Boolean.parseBoolean(reader.readLine()); //read third line for loaded

            String theme = reader.readLine(); //read fourth line for theme
            String[] themes = theme.split(",");
            wall = parseStringToTETile(themes[0]);
            floor = parseStringToTETile(themes[1]);
            nothing = parseStringToTETile(themes[2]);

            generateWorld(seed);
            player = new Character(new Coordinate(x,y), mainWorld.world, floor);

            file.delete();
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
        }
    }

    //Game saving function
    private static void saveGame() {

        //saves position of player
        StringBuilder position = new StringBuilder();
        position.append(player.getPosition().x).append(",").append(player.getPosition().y);
        StringBuilder theme = new StringBuilder();
        theme.append(wall.description().toUpperCase()).append(",").append(floor.description().toUpperCase()).append(",").append(nothing.description().toUpperCase());

        try (FileWriter writer = new FileWriter("proj3/src/core/save.txt")) {
            writer.write(userInput + System.lineSeparator()); // Write data to the file
            writer.write(position + System.lineSeparator());
            writer.write(loaded + System.lineSeparator());
            writer.write(theme + System.lineSeparator());
            System.out.println("Data saved successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while saving data: " + e.getMessage());
        }

    }

    private static void chooseTheme() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 25, "(C) Classic");
        StdDraw.text(40, 20, "(P) Plains");
        StdDraw.text(40, 15, "(V) Valleys");
        StdDraw.text(40, 10, "(B) Beach");
        StdDraw.text(40, 5, "(D) Dungeon");
        StdDraw.show();
        while (true) {
            if (StdDraw.isKeyPressed('C') || StdDraw.isKeyPressed('c')) {
                wall = Tileset.WALL;
                floor = Tileset.FLOOR;
                nothing = Tileset.NOTHING;
                break;
            }
            if (StdDraw.isKeyPressed('P') || StdDraw.isKeyPressed('p')) {
                wall = Tileset.TREE;
                floor = Tileset.GRASS;
                nothing = Tileset.FLOWER;
                break;
            }
            if (StdDraw.isKeyPressed('V') || StdDraw.isKeyPressed('v')) {
                wall = Tileset.MOUNTAIN;
                floor = Tileset.GRASS;
                nothing = Tileset.NOTHING;
                break;
            }
            if (StdDraw.isKeyPressed('B') || StdDraw.isKeyPressed('b')) {
                wall = Tileset.SAND;
                floor = Tileset.GRASS;
                nothing = Tileset.WATER;
                break;
            }
            if (StdDraw.isKeyPressed('D') || StdDraw.isKeyPressed('d')) {
                wall = Tileset.CELL;
                floor = Tileset.FLOOR;
                nothing = Tileset.NOTHING;
                break;
            }
        }
    }

    public static TETile parseStringToTETile(String tileString) {
        switch (tileString) { // Make it case-insensitive
            case "FLOOR":
                return Tileset.FLOOR;
            case "WALL":
                return Tileset.WALL;
            case "NOTHING":
                return Tileset.NOTHING;
            case "TREE":
                return Tileset.TREE;
            case "GRASS":
                return Tileset.GRASS;
            case "FLOWER":
                return Tileset.FLOWER;
            case "MOUNTAIN":
                return Tileset.MOUNTAIN;
            case "SAND":
                return Tileset.SAND;
            case "CELL":
                return Tileset.CELL;
            case "WATER":
                return Tileset.WATER;
            default:
                throw new IllegalArgumentException("Unknown tile type: " + tileString);
        }
    }
}
