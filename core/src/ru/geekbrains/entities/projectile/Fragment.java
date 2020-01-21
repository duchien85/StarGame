package ru.geekbrains.entities.projectile;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;

import ru.geekbrains.entities.objects.GameObject;
import ru.geekbrains.entities.objects.ObjectType;

public class Fragment extends Projectile  {


    public Fragment(float height, GameObject owner) {
        super(height, owner);
    }

    public Fragment(float height, float trailRadius, Color color, GameObject owner) {
        super(height, trailRadius, color, owner);
    }


    @Override
    protected void postConstruct() {

        type.add(ObjectType.FRAG);
        mass = 0.001f;
        setMaxHealth(0.002f);
        explosionRadius = 1;

        damage = 0.015f;
        penetration = 0.1f;


    }


}
