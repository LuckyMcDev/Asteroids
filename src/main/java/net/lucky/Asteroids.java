package net.lucky;

import com.jme3.app.SimpleApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

import java.util.*;

public class Asteroids extends SimpleApplication {

    private float rotationSpeed = 1f;
    private Node playerNode;
    private Geometry playerModel;

    private boolean isRightMouseDown = false;
    private float mouseSensitivity = 0.11f;
    private Vector3f angularVelocity = new Vector3f(); // pitch (X), yaw (Y), roll (Z)
    private float angularAcceleration = 5f; // how quickly it spins
    private float angularDamping = 0.92f;   // closer to 1 = slower decay

    private Vector3f velocity = new Vector3f();
    private float acceleration = 10f;
    private float damping = 0.95f; // closer to 1 = more drift
    private float yawVelocity = 0f;
    private float yawDamping = 0.5f;

    private Node sunNode;

    private List<OrbitingBody> planets = new ArrayList<>();
    private Map<OrbitingBody, List<OrbitingBody>> moons = new HashMap<>();

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

        Texture cosmos = assetManager.loadTexture("Textures/cosmos/stars.png");
        Spatial sky = SkyFactory.createSky(assetManager, cosmos, SkyFactory.EnvMapType.SphereMap);
        rootNode.attachChild(sky);

        // Simple rectangular ship model
        Box shipShape = new Box(1f, 0.25f, 2f); // wider/longer like a spaceship
        playerModel = new Geometry("Ship", shipShape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        playerModel.setMaterial(mat);

        // Attach the model to the player node
        playerNode.attachChild(playerModel);
        rootNode.attachChild(playerNode);

        // Create a directional light to represent the Sun
        DirectionalLight sunLight = new DirectionalLight();
        sunLight.setColor(ColorRGBA.White);
        sunLight.setDirection(new Vector3f(-1, -1, -1).normalizeLocal()); // Pointing towards the planets
        rootNode.addLight(sunLight);

        // Add ambient light for overall scene brightness
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.Gray);
        rootNode.addLight(ambientLight);

        // Sun and Planets setup
        sunNode = new Node("Sun");
        Sphere sunSphere = new Sphere(22, 22, 16f);
        Geometry sun = new Geometry("Sun", sunSphere);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sunMat.setColor("Color", ColorRGBA.Yellow);
        sun.setMaterial(sunMat);
        sunNode.attachChild(sun);
        rootNode.attachChild(sunNode);

        createPlanet("acid", "Acidia", 100, 0.65f, true);
        createPlanet("atmosphere", "Atmos", 130, 0.52f, false);
        createPlanet("clouds", "Nimbus", 160, 0.61f, true);
        createPlanet("craters", "Crateria", 190, 0.67f, true);
        createPlanet("haze", "Hazeon", 220, 0.44f, false);
        createPlanet("home", "Earth", 50, 0.50f, true);
        createPlanet("ice", "Glacior", 40, 0.48f, false);
        createPlanet("rock", "Petra", 110, 0.58f, true);
        createPlanet("stripes", "Striatos", 120, 0.40f, true);
        createPlanet("swirl", "Swirlia", 250, 0.30f, true);

        // Position the camera behind the player
        cam.setLocation(playerNode.getWorldTranslation().add(0, 2, -6));
        cam.lookAt(playerNode.getWorldTranslation(), Vector3f.UNIT_Y);

        // Disable the default flycam
        flyCam.setEnabled(false);

        initControls();
    }

    private void initControls() {
        inputManager.addMapping("Move_Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Move_Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Move_Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Move_Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Yaw_Left", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Yaw_Right", new KeyTrigger(KeyInput.KEY_E));

        inputManager.addMapping("Move_Up", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Move_Down", new KeyTrigger(KeyInput.KEY_LSHIFT));

        inputManager.addMapping("Mouse_Right_Down", new MouseButtonTrigger(1)); // Right-click
        inputManager.addMapping("Mouse_Move_X", new MouseAxisTrigger(0, false)); // Move mouse X+
        inputManager.addMapping("Mouse_Move_X-", new MouseAxisTrigger(0, true)); // Move mouse X-
        inputManager.addMapping("Mouse_Move_Y", new MouseAxisTrigger(1, false)); // Move mouse Y+
        inputManager.addMapping("Mouse_Move_Y-", new MouseAxisTrigger(1, true)); // Move mouse Y-

        inputManager.addListener(analogListener,
                "Move_Left", "Move_Right", "Move_Forward", "Move_Backward",
                "Yaw_Left", "Yaw_Right", "Move_Up", "Move_Down");
        inputManager.addListener(mouseListener, "Mouse_Right_Down");
        inputManager.addListener(mouseAnalogListener,
                "Mouse_Move_X", "Mouse_Move_X-", "Mouse_Move_Y", "Mouse_Move_Y-");
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
            case "Move_Up":
                velocity = velocity.add(Vector3f.UNIT_Y.mult(acceleration * tpf));
                break;
            case "Move_Down":
                velocity = velocity.subtract(Vector3f.UNIT_Y.mult(acceleration * tpf));
                break;
        }
    };

    private final com.jme3.input.controls.ActionListener mouseListener = (name, isPressed, tpf) -> {
        if (name.equals("Mouse_Right_Down")) {
            isRightMouseDown = isPressed;
            inputManager.setCursorVisible(!isPressed); // Hide cursor while dragging
        }
    };

    private final AnalogListener mouseAnalogListener = (name, value, tpf) -> {
        if (!isRightMouseDown) return;

        float delta = value * mouseSensitivity * angularAcceleration;

        switch (name) {
            case "Mouse_Move_X":
                angularVelocity.y -= delta; // yaw left
                break;
            case "Mouse_Move_X-":
                angularVelocity.y += delta; // yaw right
                break;
            case "Mouse_Move_Y":
                angularVelocity.x -= delta; // pitch down
                break;
            case "Mouse_Move_Y-":
                angularVelocity.x += delta; // pitch up
                break;
        }
    };


    private static class OrbitingBody {
        public Node node;
        public Geometry geometry;
        public float orbitRadius;
        public float orbitSpeed;
        public float orbitAngle;

        public OrbitingBody(Node node, Geometry geometry, float orbitRadius, float orbitSpeed) {
            this.node = node;
            this.geometry = geometry;
            this.orbitRadius = orbitRadius;
            this.orbitSpeed = orbitSpeed;
            this.orbitAngle = FastMath.nextRandomFloat() * FastMath.TWO_PI; // random start angle
        }
    }

    private void createPlanet(String textureName, String planetName, float orbitRadius, float orbitSpeed, boolean withMoon) {
        Node planetNode = new Node(planetName + "_Node");

        // Random size between 5 and 15 for planet, keeping it smaller than the Sun.
        float planetSize = 8f;

        // Create the planet's sphere geometry
        Sphere sphere = new Sphere(16, 16, planetSize);
        Geometry geom = new Geometry(planetName, sphere);

        // Set up a lighting material with diffuse and specular properties
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/planet/" + textureName + ".png"));
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.White);  // Set diffuse color to white
        mat.setFloat("Shininess", 16f); // Make the planet shiny for lighting effects

        geom.setMaterial(mat);
        planetNode.attachChild(geom);

        // Add the planet node to the sun's node (or the root node if you want)
        sunNode.attachChild(planetNode);

        OrbitingBody body = new OrbitingBody(planetNode, geom, orbitRadius, orbitSpeed);
        planets.add(body);

        if (withMoon) {
            createMoon(body, planetSize);
        }
    }

    private void createMoon(OrbitingBody planet, float planetSize) {
        List<OrbitingBody> moonList = new ArrayList<>();

        // Create moons around the planet
        int moonCount = 1 + FastMath.nextRandomInt(0, 2); // 1 to 3 moons
        for (int i = 0; i < moonCount; i++) {
            Node moonNode = new Node(planet.geometry.getName() + "_Moon_" + i);
            // Random moon size: Half or quarter of the planet size
            float moonSize = planetSize * 0.2f;

            // Create the moon's geometry
            Sphere moonSphere = new Sphere(8, 8, moonSize);
            Geometry moon = new Geometry("Moon_" + i, moonSphere);

            // Use the same material as planets
            Material moonMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            moonMat.setFloat("Shininess", 1f);
            moonMat.setColor("Diffuse", ColorRGBA.Gray); // Gray moon color
            moon.setMaterial(moonMat);

            moonNode.attachChild(moon);
            planet.node.attachChild(moonNode);

            // Set orbital properties for the moon
            float orbitRadius = 6f + i * 4f;
            float orbitSpeed = 1.2f + i * 0.3f;

            OrbitingBody moonBody = new OrbitingBody(moonNode, moon, orbitRadius, orbitSpeed);
            moonList.add(moonBody);
        }

        moons.put(planet, moonList);
    }





    @Override
    public void simpleUpdate(float tpf) {
        // Move the player node based on velocity
        playerNode.setLocalTranslation(playerNode.getLocalTranslation().add(velocity.mult(tpf)));

        // Smooth the velocity (simulate drifting)
        velocity.multLocal(damping);

        // Update yaw velocity to simulate drifting
        yawVelocity *= yawDamping;
        playerNode.rotate(0, yawVelocity * tpf, 0);

        // Apply angular velocity to playerNode
        // Yaw (around world Y axis)
        Quaternion yaw = new Quaternion().fromAngleAxis(angularVelocity.y * tpf, Vector3f.UNIT_Y);
        playerNode.rotate(yaw);

        // Pitch (around ship's local X axis)
        Quaternion pitch = new Quaternion().fromAngleAxis(angularVelocity.x * tpf, playerNode.getLocalRotation().mult(Vector3f.UNIT_X));
        playerNode.rotate(pitch);
        // Dampen angular velocity (drift effect)
        angularVelocity.multLocal(angularDamping);

        // Update the camera's position relative to the player
        Vector3f camOffset = new Vector3f(0, 2, -6); // position behind the player
        Vector3f camPos = playerNode.getWorldTranslation().add(playerNode.getLocalRotation().mult(camOffset));
        cam.setLocation(camPos);

        // Make the camera look at the player
        cam.lookAt(playerNode.getWorldTranslation(), Vector3f.UNIT_Y);

        for (OrbitingBody planet : planets) {
            planet.orbitAngle += planet.orbitSpeed * tpf;
            float x = planet.orbitRadius * FastMath.cos(planet.orbitAngle);
            float z = planet.orbitRadius * FastMath.sin(planet.orbitAngle);
            planet.node.setLocalTranslation(x, 0, z);

            List<OrbitingBody> moonList = moons.getOrDefault(planet, Collections.emptyList());
            for (OrbitingBody moon : moonList) {
                moon.orbitAngle += moon.orbitSpeed * tpf;
                float mx = moon.orbitRadius * FastMath.cos(moon.orbitAngle);
                float mz = moon.orbitRadius * FastMath.sin(moon.orbitAngle);
                moon.node.setLocalTranslation(mx, 0, mz);
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Optional custom rendering
    }
}

