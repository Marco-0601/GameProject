package core;
import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class World {

    private static final int WORLD_X = 90;
    private static final int WORLD_Y = 30;
    private static int ROOM_MAX_SIZE;
    Random r;
    private static TETile wall;
    private static TETile floor;
    private static TETile nothing;

    HashMap<Coordinate, Room> regions;
    List<Coordinate> floorTiles = new ArrayList<>();

    TETile[][] world;

    //Room Generation Class
    public class Room {
        Coordinate regionLocator;
        int width;
        int height;
        Coordinate roomLocator;
        int roomLocatorX;
        int roomLocatorY;

        public Room(Coordinate regionLocator, Random r) {
            this.width = 4 + r.nextInt(ROOM_MAX_SIZE - 4 + 1);
            this.height = 4 + r.nextInt(ROOM_MAX_SIZE - 4 + 1);
            this.regionLocator = regionLocator;
            if (this.width == ROOM_MAX_SIZE) {
                roomLocatorX = 0;
            } else {
                roomLocatorX = r.nextInt(ROOM_MAX_SIZE - this.width + 1);
            }

            // if room is the size of region, fix locator at bottom left.
            if (this.height == ROOM_MAX_SIZE) {
                roomLocatorY = 0;
            } else {
                roomLocatorY = r.nextInt(ROOM_MAX_SIZE - this.height + 1);
            }

            this.roomLocator = new Coordinate(roomLocatorX, roomLocatorY);

            this.updateWorld();
        }

        public void updateWorld() {
            // Update value in the 2D array World based on the room size and location
            Coordinate globalCoord = findGlobalCoord(regionLocator, roomLocator);
            for (int i = globalCoord.x; i < globalCoord.x + width; i++) {
                world[i][globalCoord.y] = wall;
                world[i][globalCoord.y + height - 1] = wall;
            }
            for (int i = globalCoord.y; i < globalCoord.y + height; i++) {
                world[globalCoord.x][i] = wall;
                world[globalCoord.x + width - 1][i] = wall;
            }
            for (int i = globalCoord.x + 1; i < globalCoord.x + width - 1; i++) {
                for (int j = globalCoord.y + 1; j < globalCoord.y + height - 1; j++) {
                    world[i][j] = floor;
                    floorTiles.add(new Coordinate(i, j));
                }
            }
        }

        public Coordinate randomCoord(int side) {
            if (side < 1 || side > 4) {
                throw new IllegalArgumentException("Input side must be a number between 1 and 4");
            }
            // returns a random coordinate on a given side. Doesn't return corner.
            // 1, 2, 3, 4 correspond to top, right, bottom, left.
            Coordinate globalCoord = findGlobalCoord(regionLocator, roomLocator);
            int xCoord;
            int yCoord;

            if (side == 1) {
                // top side random coord
                xCoord = globalCoord.x + 1 + r.nextInt(width - 2);
                yCoord = globalCoord.y + height - 1;
            } else if (side == 3) {
                // bot side random coord
                xCoord = globalCoord.x + 1 + r.nextInt(width - 2);
                yCoord = globalCoord.y;
            } else if (side == 2) {
                // right side random coord
                xCoord = globalCoord.x + width - 1;
                yCoord = globalCoord.y + 1 + r.nextInt(height - 2);
            } else {
                // left side random coord
                xCoord = globalCoord.x;
                yCoord = globalCoord.y + 1 + r.nextInt(height - 2);
            }
            return new Coordinate(xCoord, yCoord);
        }
    }

    // World constructor
    public World(long seed, TETile wallz, TETile floorz, TETile nothingz) {
        //set the theme
        wall = wallz;
        floor = floorz;
        nothing = nothingz;

        // Initialize world 2D array as all empty
        this.world = new TETile[WORLD_X][WORLD_Y];
        for (int i = 0; i < WORLD_X; i++) {
            for (int j = 0; j < WORLD_Y; j++) {
                this.world[i][j] = nothing;
            }
        }

        this.r = new Random(seed);

        int getRand = 2 + r.nextInt(2);
        ROOM_MAX_SIZE = 5 * getRand;

        System.out.println(ROOM_MAX_SIZE);

        regions = new HashMap<Coordinate, Room>();
        for (int i = 0; i < Math.floorDiv(WORLD_X, ROOM_MAX_SIZE); i++) {
            for (int j = 0; j < Math.floorDiv(WORLD_Y, ROOM_MAX_SIZE); j++) {
                Coordinate locator = new Coordinate(i, j);
                Room newRoom = new Room(locator, r);
                regions.put(locator, newRoom);
            }
        }

        this.hallwayGeneration();
    }

    public Coordinate findGlobalCoord(Coordinate regionLocator, Coordinate childLocator) {
        int xCoord = regionLocator.x * ROOM_MAX_SIZE + childLocator.x;
        int yCoord = regionLocator.y * ROOM_MAX_SIZE + childLocator.y;
        return new Coordinate(xCoord, yCoord);
    }

    public void constructHallway(Coordinate starting, Coordinate ending, Boolean isVertical) {
        // Given the starting and ending coordinates, generate floor and walls.
        // Should only be one of the following operations:
        // 1) Turn walls into floors
        // 2) Turn nothing into floors
        // 3) Turn nothing into walls
        // 4) ** SHOULD NEVER turn floors into walls
        pathMakingHelper(starting, ending, isVertical);
    }

    public void pathMakingHelper(Coordinate starting, Coordinate ending, Boolean isVertical) {
        // if the opening are right next to each other, (x-cord or y-cord of two points are same
        // when they are vertical or horizontal respectively)
        if (isVertical && starting.x == ending.x) {
            //connect them vertically
            for (int i = Math.min(starting.y, ending.y); i <= Math.max(starting.y, ending.y); i++) {
                wallMakingHelper(new Coordinate(starting.x, i));
            }
            return;
        }

        if (!isVertical && starting.y == ending.y) {
            //connect them horizontally
            for (int i = Math.min(starting.x, ending.x); i <= Math.max(starting.x, ending.x); i++) {
                wallMakingHelper(new Coordinate(i, starting.y));
            }
            return;
        }

        // vertical : 1: run, rise; 2: rise, run; 3: rise(%), run, rise(1-%)
        // horizontal : 1: run, rise; 2: rise, run; 4: run(%), rise, run(1-%)
        int randomOperationNumber = 1 + r.nextInt(2);

        if (randomOperationNumber == 1) {
            //run
            for (int i = Math.min(starting.x, ending.x); i <= Math.max(starting.x, ending.x); i++) {
                wallMakingHelper(new Coordinate(i, starting.y));
            }
            //rise
            for (int j = Math.min(starting.y, ending.y); j <= Math.max(starting.y, ending.y); j++) {
                wallMakingHelper(new Coordinate(ending.x, j));
            }
        } else {
            //rise
            for (int j = Math.min(starting.y, ending.y); j <= Math.max(starting.y, ending.y); j++) {
                wallMakingHelper(new Coordinate(starting.x, j));
            }
            //run
            for (int i = Math.min(starting.x, ending.x); i <= Math.max(starting.x, ending.x); i++) {
                wallMakingHelper(new Coordinate(i, ending.y));
            }
        }
    }

    public void coordChecker(Coordinate floorCoord) {
        if (floorCoord.x == 0 || floorCoord.x == 90 || floorCoord.y == 0 || floorCoord.y == 30) {
            System.out.println("ERROR ERROR");
        }
    }

    public void wallMakingHelper(Coordinate floorCoord) {
        // given a coordinate, make the tile 'floor', and turn every 'nothing' around into 'wall'
        world[floorCoord.x][floorCoord.y] = floor;
        for (int i = floorCoord.x - 1; i <= floorCoord.x + 1; i++) {
            for (int j = floorCoord.y - 1; j <= floorCoord.y + 1; j++) {
                if (world[i][j] != floor) {
                    world[i][j] = wall;
                }
            }
        }

    }

    public void hallwayGeneration() {
        // Given the world with rooms created, generate hallways.
        // Call constructHallways() iteratively.
        Room room1;
        Room room2;
        // Generate vertical hallways
        for (int i = 0; i < Math.floorDiv(WORLD_X, ROOM_MAX_SIZE); i++) {
            for (int j = 0; j < Math.floorDiv(WORLD_Y, ROOM_MAX_SIZE) - 1; j++) {
                room1 = regions.get(new Coordinate(i, j));
                room2 = regions.get(new Coordinate(i, j + 1));
                this.constructHallway(room1.randomCoord(1), room2.randomCoord(3), true);
            }
        }

        // Generate horizontal hallways
        for (int i = 0; i < Math.floorDiv(WORLD_X, ROOM_MAX_SIZE) - 1; i++) {
            int bridgeNum = r.nextInt(Math.floorDiv(WORLD_Y, ROOM_MAX_SIZE)); //
            room1 = regions.get(new Coordinate(i, bridgeNum));
            room2 = regions.get(new Coordinate(i + 1, bridgeNum));
            this.constructHallway(room1.randomCoord(2), room2.randomCoord(4), false);
        }
    }

    public Coordinate getRandomFloorTile() {
        // returns a random floor coordinate
        return floorTiles.get(r.nextInt(floorTiles.size()));
    }

    public TETile[][] getWorldWithCharacter(Character player) {
        TETile[][] worldWithCharacter = new TETile[WORLD_X][WORLD_Y];
        Coordinate characterPosition = player.getPosition();

        // Copy the current world
        for (int j = 0; j < WORLD_Y; j++) {
            for (int i = 0; i < WORLD_X; i++) {
                worldWithCharacter[i][j] = world[i][j];
            }
        }

        // Overlay the character
        worldWithCharacter[characterPosition.x][characterPosition.y] = Tileset.AVATAR;
        return worldWithCharacter;
    }

    public TETile[][] getWorldDark(Character player) {
        // get the world that's within line of sight.
        Coordinate characterPosition = player.getPosition();
        int characterDirection = player.getDirection();

        int lineOfSight = 5;
        TETile[][] darkWorld = new TETile[WORLD_X][WORLD_Y];

        // Only render everything in a lineOfSight x lineOfSight grid
        for (int i = 0; i < WORLD_X; i++) {
            for (int j = 0; j < WORLD_Y; j++) {
                darkWorld[i][j] = Tileset.NOTHING;
            }
        }

        // render visual obstructions
        if (characterDirection == 0) {
            for (int x = characterPosition.x - lineOfSight; x <= characterPosition.x + lineOfSight; x++) {
                for (int y = characterPosition.y + Math.abs(x - characterPosition.x); y <= characterPosition.y + lineOfSight; y++) {
                    if (inWorld(new Coordinate(x, y))) {
                        if (world[x][y] == wall) {
                            darkWorld[x][y] = wall;
                            break;
                        } else {
                            darkWorld[x][y] = world[x][y];
                        }
                    }
                }
            }
            for (int y = characterPosition.y; y <= characterPosition.y + lineOfSight; y++) {
                if (inWorld(new Coordinate(characterPosition.x, y)) && world[characterPosition.x][y] != wall){
                    for (int x = characterPosition.x; x <= characterPosition.x + Math.abs(y - characterPosition.y); x++) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    for (int x = characterPosition.x; x >= characterPosition.x - Math.abs(y - characterPosition.y); x--) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        } else if (characterDirection == 2) {
            for (int x = characterPosition.x - lineOfSight; x <= characterPosition.x + lineOfSight; x++) {
                for (int y = characterPosition.y - Math.abs(x - characterPosition.x); y >= characterPosition.y - lineOfSight; y--) {
                    if (inWorld(new Coordinate(x, y))) {
                        if (world[x][y] == wall) {
                            darkWorld[x][y] = wall;
                            break;
                        } else {
                            darkWorld[x][y] = world[x][y];
                        }
                    }
                }
            }
            for (int y = characterPosition.y; y >= characterPosition.y - lineOfSight; y--) {
                if (inWorld(new Coordinate(characterPosition.x, y)) && world[characterPosition.x][y] != wall){
                    for (int x = characterPosition.x; x <= characterPosition.x + Math.abs(y - characterPosition.y); x++) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    for (int x = characterPosition.x; x >= characterPosition.x - Math.abs(y - characterPosition.y); x--) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        } else if (characterDirection == 1) {
            for (int y = characterPosition.y - lineOfSight; y <= characterPosition.y + lineOfSight; y++) {
                for (int x = characterPosition.x + Math.abs(y - characterPosition.y); x <= characterPosition.x + lineOfSight; x++) {
                    if (inWorld(new Coordinate(x, y))) {
                        if (world[x][y] == wall) {
                            darkWorld[x][y] = wall;
                            break;
                        } else {
                            darkWorld[x][y] = world[x][y];
                        }
                    }
                }
            }
            for (int x = characterPosition.x; x <= characterPosition.x + lineOfSight; x++) {
                if (inWorld(new Coordinate(x, characterPosition.y)) && world[x][characterPosition.y] != wall){
                    for (int y = characterPosition.y; y <= characterPosition.y + Math.abs(x - characterPosition.x); y++) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    for (int y = characterPosition.y; y >= characterPosition.y - Math.abs(x - characterPosition.x); y--) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        } else {
            for (int y = characterPosition.y - lineOfSight; y <= characterPosition.y + lineOfSight; y++) {
                for (int x = characterPosition.x - Math.abs(y - characterPosition.y); x >= characterPosition.x - lineOfSight; x--) {
                    if (inWorld(new Coordinate(x, y))) {
                        if (world[x][y] == wall) {
                            darkWorld[x][y] = wall;
                            break;
                        } else {
                            darkWorld[x][y] = world[x][y];
                        }
                    }
                }
            }
            for (int x = characterPosition.x; x >= characterPosition.x - lineOfSight; x--) {
                if (inWorld(new Coordinate(x, characterPosition.y)) && world[x][characterPosition.y] != wall){
                    for (int y = characterPosition.y; y <= characterPosition.y + Math.abs(x - characterPosition.x); y++) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    for (int y = characterPosition.y; y >= characterPosition.y - Math.abs(x - characterPosition.x); y--) {
                        if (inWorld(new Coordinate(x, y))) {
                            darkWorld[x][y] = world[x][y];
                            if (world[x][y] == wall) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }

        darkWorld[characterPosition.x][characterPosition.y] = Tileset.AVATAR;
        return darkWorld;
    }

    public TETile[][] getOmniWorldDark(Character player) {
        Coordinate characterPosition = player.getPosition();

        int lineOfSight = 5;
        TETile[][] darkWorld = new TETile[WORLD_X][WORLD_Y];

        // Only render everything in a lineOfSight x lineOfSight grid
        for (int i = 0; i < WORLD_X; i++) {
            for (int j = 0; j < WORLD_Y; j++) {
                if (i >= characterPosition.x - lineOfSight && i <= characterPosition.x + lineOfSight
                        && j >= characterPosition.y - lineOfSight && j <= characterPosition.y + lineOfSight) {
                    darkWorld[i][j] = world[i][j];
                } else {
                    darkWorld[i][j] = Tileset.NOTHING;
                }
            }
        }

        darkWorld[characterPosition.x][characterPosition.y] = Tileset.AVATAR;
        return darkWorld;
    }

    public boolean inWorld(Coordinate coord) {
        return (coord.x >= 1 && coord.x < WORLD_X) && (coord.y >= 1 && coord.y < WORLD_Y);
    }
}
