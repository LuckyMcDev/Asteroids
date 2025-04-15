package net.lucky;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

public class Asteroids extends SimpleApplication {

    private float speed = 10f;
    private float rotationSpeed = 1f;
    private Node playerNode;
    private Geometry playerModel;

    private Vector3f velocity = new Vector3f();
    private float acceleration = 10f;
    private float damping = 0.95f; // closer to 1 = more drift
    private float yawVelocity = 0f; // Drift for yaw rotation
    private float yawDamping = 0.5f; // How much yaw decays over time


    private float earthAngle = 0f;
    private float marsAngle = 0f;
    private float iceAngle = 0f;

    private Node sunNode;
    private float earthOrbitSpeed = 5.05f;
    private float marsOrbitSpeed = 3.03f;
    private float iceOrbitSpeed = 2.02f;

    private float earthOrbitRadius = 100f;
    private float marsOrbitRadius = 200f;
    private float iceOrbitRadius = 80f;

    private Geometry earth, mars, ice;

    public static void main(String[] args) {
        Asteroids app = new Asteroids();
        AppSettings appSettings = new AppSettings(true);
        appSettings.setResizable(true);
        app.setSettings(appSettings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Create a node for the player
        playerNode = new Node("Player");

        cam.setFrustumPerspective(100f, (float) cam.getWidth() / cam.getHeight(), 1f, 1000f);

        // Simple rectangular ship model
        Box shipShape = new Box(1f, 0.25f, 2f); // wider/longer like a spaceship
        playerModel = new Geometry("Ship", shipShape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        playerModel.setMaterial(mat);

        // Attach the model to the player node
        playerNode.attachChild(playerModel);
        rootNode.attachChild(playerNode);

        // Sun and Planets setup
        sunNode = new Node("Sun");
        Sphere sunSphere = new Sphere(12, 12, 8f);
        Geometry sun = new Geometry("Sun", sunSphere);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sunMat.setColor("Color", ColorRGBA.Yellow);
        sun.setMaterial(sunMat);
        sunNode.attachChild(sun);
        rootNode.attachChild(sunNode);

        // Earth-like planet
        Sphere earthSphere = new Sphere(8, 8, 4f);
        earth = new Geometry("Earth", earthSphere);
        Material earthMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        earthMat.setColor("Color", ColorRGBA.Blue);
        earth.setMaterial(earthMat);
        earth.setLocalTranslation(earthOrbitRadius, 0, 0); // Initial position
        rootNode.attachChild(earth);

        // Mars-like planet
        Sphere marsSphere = new Sphere(8, 8, 3f);
        mars = new Geometry("Mars", marsSphere);
        Material marsMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        marsMat.setColor("Color", ColorRGBA.Red);
        mars.setMaterial(marsMat);
        mars.setLocalTranslation(marsOrbitRadius, 0, 0); // Initial position
        rootNode.attachChild(mars);

        // Ice Planet
        Sphere iceSphere = new Sphere(8, 8, 6f);
        ice = new Geometry("IcePlanet", iceSphere);
        Material iceMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        iceMat.setColor("Color", ColorRGBA.Cyan);
        ice.setMaterial(iceMat);
        ice.setLocalTranslation(-iceOrbitRadius, 0, 0); // Initial position
        rootNode.attachChild(ice);

        // Position the camera behind the player
        cam.setLocation(playerNode.getWorldTranslation().add(0, 2, -6));
        cam.lookAt(playerNode.getWorldTranslation(), Vector3f.UNIT_Y);

        // Disable the default flycam
        flyCam.setEnabled(false);

        initControls();
    }

    private void initControls() {
        inputManager.addMapping("Move_Left", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Move_Right", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Move_Forward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Move_Backward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Yaw_Left", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Yaw_Right", new KeyTrigger(KeyInput.KEY_E));

        inputManager.addListener(analogListener,
                "Move_Left", "Move_Right", "Move_Forward", "Move_Backward",
                "Yaw_Left", "Yaw_Right");
    }

    private final AnalogListener analogListener = (name, value, tpf) -> {
        Vector3f forward = cam.getDirection().clone().setY(0).normalizeLocal(); // Remove Y component
        Vector3f left = cam.getLeft().clone().setY(0).normalizeLocal(); // Remove Y component

        switch (name) {
            case "Move_Forward":
                velocity = velocity.add(forward.mult(acceleration * tpf));
                break;
            case "Move_Backward":
                velocity = velocity.subtract(forward.mult(acceleration * tpf));
                break;
            case "Move_Left":
                velocity = velocity.add(left.mult(acceleration * tpf));
                break;
            case "Move_Right":
                velocity = velocity.subtract(left.mult(acceleration * tpf));
                break;
            case "Yaw_Left":
                yawVelocity = rotationSpeed;
                break;
            case "Yaw_Right":
                yawVelocity = -rotationSpeed;
                break;
        }
    };

    @Override
    public void simpleUpdate(float tpf) {
        // Move the player node based on velocity
        playerNode.setLocalTranslation(playerNode.getLocalTranslation().add(velocity.mult(tpf)));

        // Smooth the velocity (simulate drifting)
        velocity.multLocal(damping);

        // Update yaw velocity to simulate drifting
        yawVelocity *= yawDamping;
        playerNode.rotate(0, yawVelocity * tpf, 0);

        // Update the camera's position relative to the player
        Vector3f camOffset = new Vector3f(0, 2, -6); // position behind the player
        Vector3f camPos = playerNode.getWorldTranslation().add(playerNode.getLocalRotation().mult(camOffset));
        cam.setLocation(camPos);

        // Make the camera look at the player
        cam.lookAt(playerNode.getWorldTranslation(), Vector3f.UNIT_Y);

        // Accumulate angles
        earthAngle += earthOrbitSpeed * tpf;
        marsAngle += marsOrbitSpeed * tpf;
        iceAngle += iceOrbitSpeed * tpf;

        // Earth orbiting the sun
        earth.setLocalTranslation(
                earthOrbitRadius * FastMath.cos(earthAngle),
                0,
                earthOrbitRadius * FastMath.sin(earthAngle)
        );

        // Mars orbiting the sun
        mars.setLocalTranslation(
                marsOrbitRadius * FastMath.cos(marsAngle),
                0,
                marsOrbitRadius * FastMath.sin(marsAngle)
        );

        // Ice Planet orbiting the sun
        ice.setLocalTranslation(
                iceOrbitRadius * FastMath.cos(iceAngle),
                0,
                iceOrbitRadius * FastMath.sin(iceAngle)
        );
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Optional custom rendering
    }
}

