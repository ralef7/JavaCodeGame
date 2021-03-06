package edu.uchicago.cs.java.finalproject.game.model;

import edu.uchicago.cs.java.finalproject.controller.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Robert on 5/23/2015.
 */
//Extends asteroid.  Basically, debris is visible as 50 randomly scattered asteroids of radius 1.
public class Debris extends Asteroid {
    private Color color;

    public Debris(Sprite deadSprite, Point startSprite, Point endSprite) {
        super(1);
        color = Color.WHITE;
        setExpire(15);


        //everything is relative to the object that explodes
        int xAdjuster = Game.R.nextInt(50);
        int yAdjuster = Game.R.nextInt(50);
        Point mid = new Point((int) ((startSprite.getX() + xAdjuster) + (endSprite.getX() + xAdjuster*2)) / 2, (int) (startSprite.getY() + yAdjuster + endSprite.getY() + yAdjuster*1.75) / 2);
        setDeltaX(deadSprite.getDeltaX() + (mid.x - deadSprite.getCenter().x) / 6);
        setDeltaY(deadSprite.getDeltaY() + (mid.y - deadSprite.getCenter().y) / 6);

        setCenter(mid);
        setRadius(1);

        double orientation = CommandCenter.getFalcon().getOrientation();
        setOrientation((int) Math.toDegrees(orientation));

        double[] spotLength = {.5, .5};

        double[] degrees = {
                Math.PI / 2, 90 * Math.PI / 200 + Math.PI};
        setLengths(spotLength);
        setDegrees(degrees);

        setFadeValue(1000);
        this.setColor(color);

        assignRandomShape();
        assignRandomShape();
        assignRandomShape();
        assignRandomShape();
    }

    public void fadeInOut() {
        if (getFadeValue() > 20) {
            setFadeValue(getFadeValue() - 10);
        }
    }

    public void move() {
        super.move();
        setOrientation(getOrientation());
    }

    public void expire() {
        if (getExpire() == 0) {
            CommandCenter.movDebris.remove(this);
        } else {
            setExpire(getExpire() - 1);
        }
    }
}

