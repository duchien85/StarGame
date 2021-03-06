package ru.geekbrains.entities.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

import ru.geekbrains.entities.particles.SmokeTrailList;
import ru.geekbrains.screen.Renderer;
import ru.geekbrains.entities.particles.SmokeTrail;
import ru.geekbrains.screen.RendererType;

/**
 * Object with thruster and gyrodine
 */
public abstract class DrivenObject extends GameObject implements SmokeTrailList {


    public float maxFuel = 100000f;        // maximum fuel tank capacity
    public float maxThrottle = 50f;        // maximum thruster engine force
    public float maxRotationSpeed = 0.05f; // maximum rotation speed

    //public Guidance guidance = Guidance.AUTO;

    public GameObject target;                       // цель

    protected Vector2 guideVector = new Vector2(); // вектор куда нужно целиться

    public float throttle = 0;                   // current throttle

    public float fuel = maxFuel;                   // current fuel level
    public float  fuelConsumption = 1;


    protected Vector2 enginePos = new Vector2();         // tail position
    public Vector2 engineTrailPos = new Vector2();    // tail position

    public List<SmokeTrail> smokeTrailList = new ArrayList<>();


    public SmokeTrail engineTrail;                      // trail from thruster burst
    public SmokeTrail damageBurnTrail;                  // trail from burning on damage

    public DrivenObject(TextureRegion textureRegion, float height, GameObject owner) {
        super(textureRegion, height, owner);

        this.type.add(ObjectType.DRIVEN_OBJECT);

        engineTrail = new SmokeTrail(radius * 0.4f * aspectRatio, new Color(0.5f, 0.5f, 0.5f, 1), this);
        engineTrail.TTL = 50;
        smokeTrailList.add(engineTrail);

        damageBurnTrail = new SmokeTrail(radius * 0.4f * aspectRatio, new Color(0.3f, 0.2f, 0.2f, 1), this);
        damageBurnTrail.TTL = 50;
        damageBurnTrail.speed = 0;
        damageBurnTrail.isStatic = true;
        smokeTrailList.add(damageBurnTrail);

        guideVector.setZero();

        rendererType.add(RendererType.TEXTURE);
        rendererType.add(RendererType.SHAPE);

    }


    /**
     * Perform simulation step
     *
     * @param dt time elapsed from previous emulation step
     */
    public void update(float dt) {


        // ~~~~~~~~~~~~~~
        super.update(dt);
        // ~~~~~~~~~~~~~~

        // auto removing destroyed targets
        if (target == null || target.readyToDispose) {
            target = null;
        }


        // guiding - controlling direction and thrust value
        guide(dt);

        // apply thruster --------------------------------------------------------------------------

        // check fuel
        if (fuel <= 0) {
            throttle = 0;
        } else {
            // update fuel
            fuel -= (throttle / maxThrottle * fuelConsumption * dt);
        }

        // apply throttle
        force.add(tmp1.set(dir).nor().scl(throttle));

        // tail vector
        tailVec.set(dir);
        tailVec.scl(-radius * aspectRatio);

        // tail position
        tailPos.set(pos).add(tailVec);


        // engine burst pos
        tmp1.set(tailVec).scl(1.3f);
        enginePos.set(pos).add(tmp1);

        // smoke trace pos
        tmp1.set(tailVec).scl(1.7f);
        engineTrailPos.set(pos).add(tmp1);

        engineTrail.setTrailPos(engineTrailPos);
        damageBurnTrail.setTrailPos(pos);


        for (SmokeTrail st : smokeTrailList) {
            st.update(dt);
        }

        // add smoke trail particle
        if (throttle > 0) {
            engineTrail.add(throttle / maxThrottle);
        }

        // add damage rail particle
        if (health < getMaxHealth()) {
            damageBurnTrail.add((getMaxHealth() - health) / maxHealth);
        }



        // auto-repair for ships
        if (health < getMaxHealth() &&
                type.contains(ObjectType.SHIP)) {
            health += 0.001;
        }



    }

    // rotation dynamics --------------------------------
    @Override
    protected void rotateObject() {

        if(!guideVector.isZero()){

            // angle between direction and guideVector
            float guideAngle = dir.angleRad(guideVector);

            float doAngle = Math.min(Math.abs(guideAngle), maxRotationSpeed);

            if (guideAngle < 0) {
                doAngle = -doAngle;
            }
            dir.rotateRad(doAngle);
        }

    }
    


    protected abstract void guide(float dt);

    // ---------------------------------------------------------------------------------------------



    @Override
    public void setRadius(float radius) {
        this.radius = radius;
        this.explosionRadius = radius * 2;

        for (SmokeTrail st : smokeTrailList) {

            st.radius = radius * 0.4f;

            if (this.type.contains(ObjectType.MISSILE)) {
                st.radius = 0.5f;
            }


        }
    }

    public void setMaxThrottle(float value) {
        maxThrottle = value;
        throttle = maxThrottle;
    }

    public void setMaxFuel(float value) {
        maxFuel = value;
        fuel = value;
    }

    @Override
    public List<SmokeTrail> removeSmokeTrailList() {

        List<SmokeTrail> result = smokeTrailList;
        smokeTrailList = new ArrayList<>();
        return result;
    }

    @Override
    public void stop() {

        for (SmokeTrail trail : smokeTrailList) {
            trail.stop();
        }
    }

    @Override
    public void draw(Renderer renderer) {

        super.draw(renderer);

        if (renderer.rendererType!=RendererType.SHAPE) {
            return;
        }


        // render smoke before ship
        engineTrail.draw(renderer);


        // render ship
        //super.draw(renderer);

        // render engine burst
        Gdx.gl.glLineWidth(1);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        //renderer.shape.begin();
        renderer.shape.set(ShapeRenderer.ShapeType.Filled);

        renderer.shape.setColor(1f, 0.8f, 0.2f, 1);
        renderer.shape.circle(enginePos.x, enginePos.y,
                radius * aspectRatio * 0.3f * (throttle/maxThrottle));

        Gdx.gl.glLineWidth(1);
        //renderer.shape.end();


        // render damage burn after ship
        damageBurnTrail.draw(renderer);

    }


    @Override
    public void dispose() {

        // do not dispose engineTrail and damageBurnTrail
        // they will be owned by explosion

//        for (SmokeTrail st : smokeTrailList) {
//            st.dispose();
//        }

//        engineTrail = null;
//        damageBurnTrail = null;
//
//        pos = null;
//        vel = null;
//        acc = null;
//        mass = 0;

        super.dispose();
    }



    

}
