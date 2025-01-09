package core;

import tileengine.TETile;

public class Character {
    private Coordinate position;
    private TETile[][] world;
    private int direction;
    private static TETile floor;

    public Character(Coordinate spawnPoint, TETile[][] world, TETile floorz) {
        this.position = spawnPoint;
        this.world = world;
        floor = floorz;
    }

    public void move(int dx, int dy) {
        if (dx == 0 && dy > 0) {
            this.direction = 0;
        } else if (dx > 0 && dy == 0) {
            this.direction = 1;
        } else if (dx == 0 && dy < 0) {
            this.direction = 2;
        } else {
            this.direction = 3;
        }
        int newX = position.x + dx;
        int newY = position.y + dy;

        // Check bounds and ensure the tile is walkable
        if (world[newX][newY] == floor) {
            position = new Coordinate(newX, newY);
        }
    }

    public Coordinate getPosition() {
        return position;
    }

    public int getDirection() {
        return direction;
    }
}
