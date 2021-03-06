/*
 * Copyright (C) 2018 Yin Fung Evans Chan
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mygame;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * AppState responsible for Special V-Style joint creation
 * @author Evans
 */
public class D1SCreationState extends BaseAppState {

    //refence to input manager and the main application
    private PopUpBook app;
    private InputManager inputManager;
    
    //patch that the Special V-style joint is build on
    private PopUpBookTree.PatchNode patchA; 
    private PopUpBookTree.PatchNode patchB; 
    //patch that are ancestors of patchA and patch B that are connected, and shares same nomal with patchA and patch B relatively
    private PopUpBookTree.PatchNode basePatchA;
    private PopUpBookTree.PatchNode basePatchB;
    
    //Axis used in this app state
    private Vector3f centerAxis; //an axis between the two patchA and patchB
    private Vector3f[] axisPoints; //the points that makes cennterAxis
    private Vector3f axisTranslationA; //translation from centerAxis to patchA
    private Vector3f axisTranslationB; //translation from centerAxis to patchA
    
    //Nodes
    private Node tempNode; //everything in this app state is build base on tempNode. Remove tempNode when appstate is disabled
    private Node frameNode;//Contains the frame of the patch in the proces of building
    private Node collisionNode; //Used for ray casting for mouse clicking and dragging
    
    //Material
    private Material dotMaterial;
    private Material lineMaterial;
    
    //Flags for inputs
    private String mode;
    private String dragMode;
    
    //Data for the special v-style joint patches
    private ArrayList<Vector3f> verticesA;
    private ArrayList<Vector3f> verticesB;
    private HashMap<Geometry, Vector3f> dotVecticesMap;
    private HashMap<Geometry, Vector3f[]> lineVecticesMap;
    
    //Variable for clicking using collision plane and ray
    private Geometry selected;
    private Vector3f referencePoint;
    
    //Safety
    private ArrayList<Vector3f> boundaryA;
    private ArrayList<Vector3f> boundaryB;
    private Geometry boundaryAGeom;
    private Geometry boundaryBGeom;
    
    //inputNames and inputListeners
    private final String D1S_ESCAPE = "D1S_Escape";
    private final String D1S_SELECT = "D1S_Select";
    private final String D1S_ADD = "D1S_ADD";
    private final String D1S_MOUSE_MOVE = "D1S_MouseMove";
    private final String D1S_CONFIRM = "D1S_Confirm";
    private final ActionListener d1SBasicInput = new D1SBasicListener();
    private final D1SMoustListener d1SMouseListener = new D1SMoustListener();
    
    //Constant for line mesh and sphere mesh.
    private final float lineRadius = 0.05f;
    private final float sphereRadius = 0.125f;

    /**
     * Actionlistener for mouse dragging movement 
     */
    private class D1SMoustListener implements AnalogListener {

        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (dragMode != null) {
                CollisionResults results = new CollisionResults();
                Vector2f click2d = inputManager.getCursorPosition().clone();
                Vector3f click3d = app.getCamera().getWorldCoordinates(click2d, 0f).clone();
                Vector3f dir = app.getCamera().getWorldCoordinates(click2d, 1f).subtractLocal(click3d).normalizeLocal();
                Ray ray = new Ray(click3d, dir);
                collisionNode.collideWith(ray, results);

                switch (dragMode) {
                    case "freeMove":
                        collisionNode.collideWith(ray, results);
                        if (results.size() > 0) {
                            Vector3f newPoint = results.getClosestCollision().getContactPoint();
                            ArrayList<Vector3f> pointSet;
                            if (verticesA.contains(dotVecticesMap.get(selected))) {
                                pointSet = verticesA;
                            } else {
                                pointSet = verticesB;
                            }
                            int i = pointSet.indexOf(dotVecticesMap.get(selected));
                            pointSet.get(i).set(newPoint);
                            for (int x = 0; x < pointSet.size(); x++) {
                                boolean matched = false;
                                if (x != i && x != 1 && x != 0) {
                                    Vector3f newPointVector = Util.lineToPointTranslation(pointSet.get(1), pointSet.get(0).subtract(pointSet.get(1)).normalize(), pointSet.get(i));
                                    Vector3f thisPointVector = Util.lineToPointTranslation(pointSet.get(1), pointSet.get(0).subtract(pointSet.get(1)).normalize(), pointSet.get(x));
                                    if (newPointVector.distance(thisPointVector) < 0.5f) {
                                        pointSet.get(i).addLocal(thisPointVector.subtract(newPointVector));
                                        matched = true;
                                    }
                                }
                                if (x != i && x != 2 && x != 1) {
                                    Vector3f newPointVector = Util.lineToPointTranslation(pointSet.get(1), pointSet.get(2).subtract(pointSet.get(1)).normalize(), pointSet.get(i));
                                    Vector3f thisPointVector = Util.lineToPointTranslation(pointSet.get(1), pointSet.get(2).subtract(pointSet.get(1)).normalize(), pointSet.get(x));
                                    if (newPointVector.distance(thisPointVector) < 0.5f) {
                                        pointSet.get(i).addLocal(thisPointVector.subtract(newPointVector));
                                        matched = true;
                                    }
                                }
                                if (matched || x == i) {
                                    getDot(pointSet.get(x)).getMaterial().setColor("Color", ColorRGBA.Yellow);
                                } else {
                                    getDot(pointSet.get(x)).getMaterial().setColor("Color", ColorRGBA.Red);
                                }
                            }
                            fitInBoundaries();
                            updateGraphics();
                        }
                        break;
                    case "SideAngle": {
                        collisionNode.collideWith(ray, results);
                        if (results.size() > 0) {
                            Vector3f newPoint = results.getClosestCollision().getContactPoint();
                            if (verticesA.get(0).equals(dotVecticesMap.get(selected))) {
                                Vector3f original = newPoint.subtract(verticesA.get(1));
                                float length = original.length() * FastMath.cos(original.angleBetween(verticesA.get(0).subtract(verticesA.get(1))));
                                if (length < 0.5f) {
                                    length = 0.5f;
                                }
                                verticesA.get(0).set(verticesA.get(0).subtract(verticesA.get(1)).normalize().mult(length).add(verticesA.get(1)));

                            } else {

                                Vector3f original = newPoint.subtract(verticesB.get(1));
                                float length = original.length() * FastMath.cos(original.angleBetween(verticesB.get(0).subtract(verticesB.get(1))));
                                if (length < 0.5f) {
                                    length = 0.5f;
                                }
                                verticesB.get(0).set(verticesB.get(0).subtract(verticesB.get(1)).normalize().mult(length).add(verticesB.get(1)));
                            }

                            fitInBoundaries();
                            updateGraphics();
                        }
                        break;
                    }
                    case "TopAngle": {
                        //TopAngle
                        collisionNode.collideWith(ray, results);
                        if (results.size() > 0) {
                            Vector3f newPoint = results.getClosestCollision().getContactPoint();
                            Vector3f original = newPoint.subtract(verticesA.get(1));
                            float newLength = original.length() * FastMath.cos(original.angleBetween(verticesA.get(2).subtract(verticesA.get(1))));
                            verticesA.get(2).set(verticesA.get(1).add(verticesA.get(2).subtract(verticesA.get(1)).normalize().mult(newLength)));
                            fitInBoundaries();
                            Plane plane = new Plane();
                            plane.setPlanePoints(verticesA.get(0), verticesA.get(1), verticesA.get(2));
                            for (Vector3f point : verticesA) {
                                if (!plane.isOnPlane(point)) {
                                    point.set(plane.getClosestPoint(point));
                                }
                            }
                            plane.setPlanePoints(verticesB.get(0), verticesB.get(1), verticesB.get(2));
                            for (Vector3f point : verticesB) {
                                if (!plane.isOnPlane(point)) {
                                    point.set(plane.getClosestPoint(point));
                                }
                            }
                            updateGraphics();
                        }
                        break;
                    }
                    case "shift": {
                        collisionNode.collideWith(ray, results);
                        if (results.size() > 0) {
                            Vector3f newPoint = results.getClosestCollision().getContactPoint();
                            Vector3f centerAxis = verticesA.get(0).subtract(verticesA.get(1)).normalize().add(verticesB.get(0).subtract(verticesB.get(1)).normalize());
                            Vector3f midA = verticesA.get(1).add(centerAxis);
                            Vector3f midB = verticesA.get(1).subtract(centerAxis);
                            float angle = newPoint.subtract(midA).normalize().angleBetween(midB.subtract(midA).normalize());
                            newPoint = midA.add(midB.subtract(midA).normalize().mult(FastMath.cos(angle) * newPoint.distance(midA)));
                            Vector3f translation = newPoint.subtract(referencePoint);
                            if (Util.inBoundary(verticesA.get(0).add(translation), patchA.boundary)) {
                                for (Vector3f point : verticesA) {
                                    if (!point.equals(verticesA.get(2))) {
                                        point.addLocal(translation);
                                    }
                                }
                                for (Vector3f point : verticesB) {
                                    if (!point.equals(verticesA.get(2))) {
                                        point.addLocal(translation);
                                    }
                                }
                                verticesA.get(2).addLocal(translation);
                                referencePoint.addLocal(translation);
                                updateBoundaries();
                                updateGraphics();
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }

            }

        }

    }

    /**
     * ActionListener for key's and clicks 
     */
    private class D1SBasicListener implements ActionListener {

        @Override
        public void onAction(String action, boolean isPressed, float tpf) {
            switch (action) {
                case D1S_CONFIRM: {
                    if (isPressed) {
                        Vector3f[] boundaryA = verticesA.toArray(new Vector3f[verticesA.size()]);
                        Vector3f[] boundaryB = verticesB.toArray(new Vector3f[verticesB.size()]);

                        PopUpBookTree.PatchNode newPatchA = app.popUpBook.addPatch(patchA.geometry, boundaryA, new Vector3f[]{verticesA.get(0).clone(), verticesA.get(1).clone()});
                        PopUpBookTree.PatchNode newPatchB = app.popUpBook.addPatch(patchB.geometry, boundaryB, new Vector3f[]{verticesB.get(0).clone(), verticesB.get(1).clone()});
                        app.popUpBook.addJoint(newPatchA, newPatchB, new Vector3f[]{verticesA.get(2), verticesA.get(1)}, "D1Joint");

                        app.getStateManager().getState(ExplorationState.class).setEnabled(true);
                        setEnabled(false);
                    }
                    break;
                }

                case D1S_ADD: {
                    if (isPressed) {
                        mode = D1S_ADD;
                    } else {
                        mode = null;
                    }
                    break;
                }

                case D1S_ESCAPE: {
                    if (isPressed) {
                        for (Geometry plane : app.selected) {
                            plane.setMaterial(app.paper);
                        }
                        app.selected.clear();
                        setEnabled(false);
                        app.enableState(ExplorationState.class, true);
                    }
                    break;
                }

                case D1S_SELECT: {
                    if (isPressed) {
                        CollisionResults results = new CollisionResults();
                        Vector2f click2d = inputManager.getCursorPosition().clone();
                        Vector3f click3d = app.getCamera().getWorldCoordinates(click2d, 0f).clone();
                        Vector3f dir = app.getCamera().getWorldCoordinates(click2d, 1f).subtractLocal(click3d).normalizeLocal();
                        Ray ray = new Ray(click3d, dir);
                        frameNode.collideWith(ray, results);
                        if (results.size() > 0) {
                            app.chaseCam.setEnabled(false);
                            app.getInputManager().setCursorVisible(true);
                            collisionNode.detachAllChildren();
                            CollisionResult closest = results.getClosestCollision();
                            if (mode == D1S_ADD) {
                                if (closest.getGeometry().getName().equals("Line")) {
                                    Geometry line = closest.getGeometry();
                                    Vector3f[] points = lineVecticesMap.get(line);
                                    if (verticesA.get(1) != points[0] && verticesA.get(1) != points[1]) {
                                        Vector3f h = closest.getContactPoint().subtract(points[0]);
                                        Vector3f o = points[1].subtract(points[0]).normalize();
                                        o = points[0].add(o.mult(FastMath.cos(h.normalize().angleBetween(o)) * h.length()));
                                        if (verticesA.contains(points[0]) && verticesA.contains(points[1])) {
                                            int index = Math.max(verticesA.indexOf(points[0]), verticesA.indexOf(points[1]));
                                            if (verticesA.indexOf(points[0]) == 0 || verticesA.indexOf(points[1]) == 0) {
                                                verticesA.add(o);
                                            } else {
                                                verticesA.add(index, o);
                                            }
                                        } else {
                                            int index = Math.max(verticesB.indexOf(points[0]), verticesB.indexOf(points[1]));
                                            if (verticesB.indexOf(points[0]) == 0 || verticesB.indexOf(points[1]) == 0) {
                                                verticesB.add(o);
                                            } else {
                                                verticesB.add(index, o);
                                            }
                                        }

                                        addDot(o);
                                        addLine(points[1], o);
                                        ((Cylinder) line.getMesh()).updateGeometry(5, 3, lineRadius, lineRadius, points[0].distance(o), false, false);
                                        line.setLocalTranslation(points[0].add(o).divide(2f));
                                        lineVecticesMap.get(line)[0] = points[0];
                                        lineVecticesMap.get(line)[1] = o;

                                    }

                                }
                            } else {
                                //drag movement
                                closest.getGeometry().getMaterial().setColor("Color", ColorRGBA.Yellow);
                                selected = closest.getGeometry();

                                switch (selected.getName()) {
                                    case "Line": {
                                        selected.getMaterial().setColor("Color", ColorRGBA.Yellow);
                                        Vector3f centerAxis = verticesA.get(0).subtract(verticesA.get(1)).normalize().add(verticesB.get(0).subtract(verticesB.get(1)).normalize());
                                        Vector3f topA = verticesA.get(1).add(centerAxis.normalize().mult(100)).add(verticesA.get(0).subtract(verticesB.get(0)).normalize().mult(50f));
                                        Vector3f topB = verticesA.get(1).subtract(centerAxis.normalize().mult(100)).add(verticesA.get(0).subtract(verticesB.get(0)).normalize().mult(50f));
                                        Vector3f botB = verticesA.get(1).subtract(centerAxis.normalize().mult(100)).add(verticesB.get(0).subtract(verticesA.get(0)).normalize().mult(50f));
                                        Vector3f botA = verticesA.get(1).add(centerAxis.normalize().mult(100)).add(verticesB.get(0).subtract(verticesA.get(0)).normalize().mult(50f));
                                        Vector3f[] temp = {botA, botB, topB, topA};
                                        Geometry collision = new Geometry("Collision", Util.makeMesh(temp));
                                        collision.setMaterial(dotMaterial);
                                        collisionNode.attachChild(collision);
                                        //app.getRootNode().attachChild(collision);
                                        results.clear();
                                        collisionNode.collideWith(ray, results);
                                        if (results.size() > 0) {
                                            referencePoint = results.getClosestCollision().getContactPoint();
                                            Vector3f midA = verticesA.get(1).add(centerAxis);
                                            Vector3f midB = verticesA.get(1).subtract(centerAxis);
                                            float angle = referencePoint.subtract(midA).normalize().angleBetween(midB.subtract(midA).normalize());
                                            referencePoint = midA.add(midB.subtract(midA).normalize().mult(FastMath.cos(angle) * referencePoint.distance(midA)));
                                            dragMode = "shift";
                                        }
                                        break;
                                    }
                                    case "Dot": {
                                        Vector3f selectedVertex = dotVecticesMap.get(selected);
                                        if (selectedVertex.equals(verticesA.get(1))) {
                                            //center point

                                        } else if (selectedVertex.equals(verticesA.get(0)) || selectedVertex.equals(verticesB.get(0))) {
                                            //side point
                                            dragMode = "SideAngle";
                                            Vector3f centerTop;
                                            Vector3f centerBot;
                                            Vector3f sideTop;
                                            Vector3f sideBot;
                                            if (verticesA.contains(selectedVertex)) {
                                                centerTop = verticesA.get(1).add(centerAxis.normalize().mult(100));
                                                centerBot = verticesA.get(1).subtract(centerAxis.normalize().mult(100));
                                                sideTop = centerTop.add(axisTranslationA.normalize().mult(100));
                                                sideBot = centerBot.add(axisTranslationA.normalize().mult(100));
                                            } else {
                                                centerTop = verticesB.get(1).add(centerAxis.normalize().mult(100));
                                                centerBot = verticesB.get(1).subtract(centerAxis.normalize().mult(100));
                                                sideTop = centerTop.add(axisTranslationB.normalize().mult(100));
                                                sideBot = centerBot.add(axisTranslationB.normalize().mult(100));
                                            }

                                            Vector3f[] temp = {centerTop, centerBot, sideBot, sideTop};
                                            Geometry collision = new Geometry("Collision", Util.makeMesh(temp));
                                            collision.setMaterial(dotMaterial);
                                            collisionNode.attachChild(collision);

                                        } else if (selectedVertex.equals(verticesA.get(2)) || selectedVertex.equals(verticesB.get(2))) {
                                            //top point
                                            dragMode = "TopAngle";
                                            Vector3f botA = verticesA.get(1).add(centerAxis.normalize().mult(100f));
                                            Vector3f botB = verticesA.get(1).subtract(centerAxis.normalize().mult(100f));
                                            Vector3f up = verticesA.get(0).subtract(verticesA.get(1)).cross(verticesB.get(0).subtract(verticesB.get(1))).normalize();
                                            if (up.add(verticesA.get(1)).distance(verticesA.get(2)) > up.negate().add(verticesA.get(1)).distance(verticesA.get(2))) {
                                                up.negateLocal();
                                            }
                                            Vector3f topA = botA.add(up.mult(100f));
                                            Vector3f topB = botB.add(up.mult(100f));

                                            Vector3f[] temp = {botA, botB, topB, topA};
                                            for (Vector3f point : temp) {
                                                addDot(point);
                                            }
                                            Geometry collision = new Geometry("Collision", Util.makeMesh(temp));
                                            collision.setMaterial(dotMaterial);
                                            collisionNode.attachChild(collision);
                                        } else {
                                            //free movement
                                            dragMode = "freeMove";

                                            Vector3f up = verticesA.get(2).subtract(verticesA.get(1));
                                            up = verticesA.get(1).add(up.normalize().mult(100));
                                            Vector3f side;
                                            if (verticesA.contains(selectedVertex)) {
                                                side = verticesA.get(0).subtract(verticesA.get(1));
                                            } else {
                                                side = verticesB.get(0).subtract(verticesB.get(1));

                                            }
                                            side = verticesA.get(1).add(side.normalize().mult(100));
                                            Vector3f[] temp = {up, verticesA.get(1), side};

                                            Geometry collision = new Geometry("Collision", Util.makeMesh(temp));
                                            collision.setMaterial(dotMaterial);
                                            collisionNode.attachChild(collision);
                                        }
                                        break;
                                    }
                                    default:
                                        break;
                                }

//
                            }
                        }

                    } else {
                        if (selected != null) {
                            if (selected.getName().equals("Dot")) {
                                selected.getMaterial().setColor("Color", ColorRGBA.Red);
                            } else {
                                selected.getMaterial().setColor("Color", ColorRGBA.Black);
                            }
                            selected = null;
                        }
                        collisionNode.detachAllChildren();
                        app.chaseCam.setEnabled(true);
                        dragMode = null;
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * Constructor for this AppState
     * @param b 
     */
    D1SCreationState(boolean b) {
        setEnabled(b);
    }

    /**
     * intializes the appstate with the main application. Setting up input, and materials
     * @param app 
     */
    @Override
    protected void initialize(Application app) {
        this.app = (PopUpBook) app;
        inputManager = app.getInputManager();
        inputManager.addMapping(D1S_ESCAPE, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping(D1S_SELECT, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(D1S_ADD, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(D1S_CONFIRM, new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_NUMPADENTER));
        inputManager.addMapping(D1S_MOUSE_MOVE, new MouseAxisTrigger(MouseInput.AXIS_X, true), new MouseAxisTrigger(MouseInput.AXIS_X, false));

        dotMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        dotMaterial.setColor("Color", ColorRGBA.Red);
        lineMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        lineMaterial.setColor("Color", ColorRGBA.Black);

    }

    @Override
    protected void cleanup(Application app) {
    }

    /**
     * enable the app state setting remapping the inputlistener, and set up instruction and mode text.
     */
    @Override
    protected void onEnable() {
        tempNode = new Node("temp");
        frameNode = new Node("frame");
        collisionNode = new Node("collision");
        boundaryAGeom = null;
        boundaryBGeom = null;
        tempNode.attachChild(frameNode);
        lineVecticesMap = new HashMap();
        dotVecticesMap = new HashMap();
        this.app.getRootNode().attachChild(tempNode);
        inputManager.addListener(d1SBasicInput, D1S_ESCAPE);
        inputManager.addListener(d1SBasicInput, D1S_SELECT);
        inputManager.addListener(d1SBasicInput, D1S_ADD);
        inputManager.addListener(d1SBasicInput, D1S_CONFIRM);
        inputManager.addListener(d1SMouseListener, D1S_MOUSE_MOVE);
        app.setText("Instruction", "-[Enter]   To confirm\n"
                                    +"-[ESC]     To discard\n"
                                    +"-Drag point around to shift point\n"
                                    + "-Click and Drag point around to shift point\n"
                                    + "-Hold [D] and left click lines to add points\n");
        initialize();
    }

    /**
     * Sets up the point of the patches of the step joint and build the frame for it
     */
    private void initialize() {
        patchA = app.popUpBook.geomPatchMap.get(app.selected.get(0));
        patchB = app.popUpBook.geomPatchMap.get(app.selected.get(1));

        boolean found = false;
        ArrayList<PopUpBookTree.PatchNode> aParents = new ArrayList<>();
        ArrayList<PopUpBookTree.PatchNode> bParents = new ArrayList<>();
        aParents.add(patchA);
        bParents.add(patchB);

        PopUpBookTree.PatchNode parent = patchA.parent;
        while (parent != null) {
            if (parent.getNormal().cross(patchA.getNormal()).distance(Vector3f.ZERO) < FastMath.FLT_EPSILON) {
                aParents.add(parent);
            }
            parent = parent.parent;
        }
        parent = patchB.parent;
        while (parent != null) {
            if (parent.getNormal().cross(patchB.getNormal()).distance(Vector3f.ZERO) < FastMath.FLT_EPSILON) {
                bParents.add(parent);
            }
            parent = parent.parent;
        }
        outerLoop:
        for (PopUpBookTree.PatchNode aPatch : aParents) {
            for (PopUpBookTree.PatchNode bPatch : bParents) {
                if (app.popUpBook.isNeighbor(aPatch.geometry, bPatch.geometry)) {
                    found = true;
                    basePatchA = aPatch;
                    basePatchB = bPatch;
                    axisPoints = app.popUpBook.axisBetween(aPatch.geometry, bPatch.geometry);
                    centerAxis = axisPoints[0].subtract(axisPoints[1]);
                    break outerLoop;
                }
            }
        }
        if (found) {
            if (patchA.distanceFromPoint(basePatchA.boundary[0]) < patchB.distanceFromPoint(basePatchB.boundary[0])) {
                PopUpBookTree.PatchNode temp = patchA;
                patchA = patchB;
                patchB = temp;
                temp = basePatchA;
                basePatchA = basePatchB;
                basePatchB = temp;
            }
            app.setText("Mode", "Special V-Style Joint Creation Mode");
            for (Vector3f point : basePatchA.boundary) {
                if (!Util.inLine(axisPoints[0], point, axisPoints[1])) {
                    axisTranslationA = Util.lineToPointTranslation(axisPoints[0], centerAxis, point).normalize();
                    break;
                }
            }

            for (Vector3f point : basePatchB.boundary) {
                if (!Util.inLine(axisPoints[0], point, axisPoints[1])) {
                    axisTranslationB = Util.lineToPointTranslation(axisPoints[0], centerAxis, point).normalize();
                    break;
                }
            }
            System.out.println("aAxis + " + axisTranslationA);
            System.out.println("bAxis + " + axisTranslationB);
            Plane midPlane = new Plane();
            midPlane.setOriginNormal(axisPoints[1], axisTranslationA.subtract(axisTranslationB).normalize());
            System.out.println("Normal + " + midPlane.getNormal());
            Vector3f sideNormal = null;

            if (patchA.joint.type.equals("D2Joint")) {
                sideNormal = patchA.joint.theOther(patchA).getNormal();
                Vector3f[] axis = patchA.axis;
                if (FastMath.abs(axis[0].distance(axisPoints[1])) < FastMath.abs(axis[1].distance(axisPoints[1]))) {
                    axisTranslationA = axis[1].subtract(axis[0]);
                } else {
                    axisTranslationA = axis[0].subtract(axis[1]);
                }
                axisTranslationB = midPlane.reflect(axisPoints[1].add(axisTranslationA), null).subtract(axisPoints[1]);
            }

            if (sideNormal == null) {
                if (patchB.joint.type.equals("D2Joint")) {
                    sideNormal = patchB.joint.theOther(patchB).getNormal();
                    Vector3f[] axis = patchB.axis;
                    if (FastMath.abs(axis[0].distance(axisPoints[1])) < FastMath.abs(axis[1].distance(axisPoints[1]))) {
                        axisTranslationB = axis[1].subtract(axis[0]);
                    } else {
                        axisTranslationB = axis[0].subtract(axis[1]);
                    }
                    axisTranslationA = midPlane.reflect(axisPoints[1].add(axisTranslationB), null).subtract(axisPoints[1]);

                }
            }
            if (sideNormal != null) {
                verticesA = new ArrayList<>();
                verticesB = new ArrayList<>();

                verticesB.add(new Vector3f());
                System.out.println("sideNormal = " + sideNormal);
                System.out.println("midNormal = " + midPlane.getNormal().normalize());
                Vector3f up = sideNormal.normalize().cross(midPlane.getNormal().normalize()).normalize();
                Vector3f center = new Vector3f();
                for (Vector3f point : patchA.boundary) {
                    center.addLocal(point);
                }
                center.divideLocal(patchA.boundary.length);
                verticesA.add(center);
                verticesA.add(Util.linePlaneIntersection(center, axisTranslationA.negate().normalize(), axisPoints[1], midPlane.getNormal()));
                Vector3f[] limitA = new Vector3f[]{Util.linePlaneIntersection(patchA.boundary[0], axisTranslationA.negate().normalize(), axisPoints[1], midPlane.getNormal()),
                    Util.linePlaneIntersection(patchA.boundary[2], axisTranslationA.negate().normalize(), axisPoints[1], midPlane.getNormal())};

                verticesB.add(Util.linePlaneIntersection(verticesA.get(1), up.negate(), patchB.boundary[0], patchB.getNormal()));
                limitA[0].set(Util.linePlaneIntersection(limitA[0], up.negate(), patchB.boundary[0], patchB.getNormal()));
                limitA[1].set(Util.linePlaneIntersection(limitA[1], up.negate(), patchB.boundary[0], patchB.getNormal()));

                Vector3f[] limitB = new Vector3f[]{new Vector3f(), new Vector3f()};
                for (Vector3f point1 : patchB.boundary) {
                    for (Vector3f point2 : patchB.boundary) {
                        Vector3f project1 = Util.linePlaneIntersection(point1, axisTranslationB.negate().normalize(), axisPoints[1], midPlane.getNormal());
                        Vector3f project2 = Util.linePlaneIntersection(point2, axisTranslationB.negate().normalize(), axisPoints[1], midPlane.getNormal());
                        if (project1.distance(project2) >= limitB[0].distance(limitB[1])) {
                            limitB[0].set(project1);
                            limitB[1].set(project2);
                        }
                    }
                }

                Vector3f translation;
                if (Util.isBetween(limitB[0], limitA[0], limitB[1]) || Util.isBetween(limitB[0], limitA[1], limitB[1])) {
                    if (Util.isBetween(limitB[0], limitA[0], limitB[1]) && Util.isBetween(limitB[0], limitA[1], limitB[1])) {
                        //All Good
                        translation = verticesB.get(1).clone();
                    } else if (Util.isBetween(limitB[0], limitA[0], limitB[1])) {
                        //only limitA0 inside B
                        if (Util.isBetween(limitA[0], limitB[0], limitA[1])) {
                            translation = limitB[0].add(limitA[0]).divide(2);
                        } else {
                            translation = limitB[1].add(limitA[0]).divide(2);
                        }
                    } else if (Util.isBetween(limitB[0], limitA[1], limitB[1])) {
                        //onlyl limitA1 inside B
                        if (Util.isBetween(limitA[0], limitB[0], limitA[1])) {
                            translation = limitB[0].add(limitA[1]).divide(2);
                        } else {
                            translation = limitB[1].add(limitA[1]).divide(2);
                        }
                    } else {
                        //non is Inside B
                        translation = limitB[0].add(limitB[1]).divide(2);
                    }
                    translation.subtractLocal(verticesB.get(1));
                    verticesA.get(1).add(translation);
                    verticesB.get(1).add(translation);
                    Vector3f[] boundaryIntersections = Util.lineBoundaryIntersectionPair(verticesA.get(1), axisTranslationA, patchA.boundary);
                    verticesA.get(0).set(boundaryIntersections[0].add(boundaryIntersections[1]).divide(2));
                    boundaryIntersections = Util.lineBoundaryIntersectionPair(verticesB.get(1), axisTranslationB, patchB.boundary);
                    verticesB.get(0).set(boundaryIntersections[0].add(boundaryIntersections[1]).divide(2));
                    float height = verticesA.get(1).distance(verticesA.get(0)) * 1.618f / 2f;
                    if (height < FastMath.FLT_EPSILON) {
                        height = verticesA.get(1).distance(verticesB.get(1)) / 1.618f;
                    }
                    verticesA.add(up.mult(height).add(verticesA.get(1)));
                    verticesB.add(verticesA.get(2));
                    addDot(verticesA.get(0));
                    addDot(verticesA.get(1));
                    addDot(verticesB.get(0));
                    addDot(verticesB.get(1));
                    addDot(verticesA.get(2));
                    addLine(verticesA.get(0), verticesA.get(1));
                    addLine(verticesA.get(0), verticesA.get(2));
                    addLine(verticesB.get(1), verticesB.get(2));
                    addLine(verticesB.get(2), verticesB.get(0));
                    addLine(verticesB.get(1), verticesB.get(0));
                    updateBoundaries();
                    fitInBoundaries();

                } else {
                    //fail
                    app.setText("Error", "A Special /V-Style Joint cannot be built on these planes ERROR1");
                    setEnabled(false);
                    app.enableState(ExplorationState.class, true);
                }

            } else {
                //fail
                app.setText("Error", "A D1 Joint cannot be built on these planes ERROR2");
                setEnabled(false);
                app.enableState(ExplorationState.class, true);
            }
        } else {
            //fail
            app.setText("Error", "A Special V-style or V-style Joint cannot be built on these planes Error3");
            setEnabled(false);
            app.enableState(ExplorationState.class, true);
        }

    }

    /**
     * fit all vertices within the safty boundary
     */
    private void fitInBoundaries() {
        if (!boundaryA.isEmpty()) {
            float length = verticesA.get(0).distance(verticesA.get(1));
            if (length > boundaryA.get(1).distance(boundaryA.get(0))) {
                length = boundaryA.get(1).distance(boundaryA.get(0));
            }
            if (length < 0.5f) {
                length = 0.5f;
            }
            verticesA.get(0).set(verticesA.get(0).subtract(verticesA.get(1)).normalize().mult(length).add(verticesA.get(1)));

            length = verticesB.get(0).distance(verticesB.get(1));
            if (length > boundaryB.get(1).distance(boundaryB.get(0))) {
                length = boundaryB.get(1).distance(boundaryB.get(0));
            }
            if (length < 0.5f) {
                length = 0.5f;
            }
            verticesB.get(0).set(verticesB.get(0).subtract(verticesB.get(1)).normalize().mult(length).add(verticesB.get(1)));

            length = verticesA.get(1).distance(verticesA.get(2));
            if (length > boundaryA.get(1).distance(boundaryA.get(2))) {
                length = boundaryA.get(1).distance(boundaryA.get(2));
            }
            if (length < 0.5f) {
                length = 0.5f;
            }
            verticesA.get(2).set(verticesA.get(2).subtract(verticesA.get(1)).normalize().mult(length).add(verticesA.get(1)));
            Plane planeBot = new Plane();
            Plane planeTop = new Plane();

            Vector3f planeNormal = verticesA.get(0).subtract(verticesA.get(1)).cross(verticesA.get(2).subtract(verticesA.get(1)));
            planeBot.setPlanePoints(verticesA.get(0), verticesA.get(1), verticesA.get(1).add(planeNormal));
            planeTop.setPlanePoints(verticesA.get(2), verticesA.get(1), verticesA.get(1).add(planeNormal));
            for (int i = 3; i < verticesA.size(); i++) {
                if (FastMath.abs(planeBot.pseudoDistance(verticesA.get(i))) < FastMath.FLT_EPSILON || FastMath.abs(planeTop.pseudoDistance(verticesA.get(i))) < FastMath.FLT_EPSILON) {
                    //Do Nothing
                } else {
                    if (planeBot.whichSide(verticesA.get(i)).equals(planeBot.whichSide(verticesA.get(2)))
                            && planeTop.whichSide(verticesA.get(i)).equals(planeTop.whichSide(verticesA.get(0)))) {
                        for (int x = 2; x < boundaryA.size(); x++) {
                            Vector3f point1 = boundaryA.get(x);
                            Vector3f point2;
                            if (x == boundaryA.size() - 1) {
                                point2 = boundaryA.get(0);
                            } else {
                                point2 = boundaryA.get(x + 1);
                            }
                            Float angle1 = point1.subtract(verticesA.get(1)).normalize().angleBetween(verticesA.get(0).subtract(verticesA.get(1)).normalize());
                            Float angle2 = point2.subtract(verticesA.get(1)).normalize().angleBetween(verticesA.get(0).subtract(verticesA.get(1)).normalize());
                            Float targetAngle = verticesA.get(i).subtract(verticesA.get(1)).normalize().angleBetween(verticesA.get(0).subtract(verticesA.get(1)).normalize());
                            if ((angle1 > targetAngle && angle2 < targetAngle)) {
                                Plane plane = new Plane();
                                plane.setPlanePoints(point1, point2, point1.add(planeNormal));
                                if (plane.whichSide(verticesA.get(i)).equals(plane.whichSide(verticesA.get(1)))) {
                                    break;
                                } else {
                                    verticesA.get(i).set(plane.getClosestPoint(verticesA.get(i)));

                                    plane.setPlanePoints(point1, boundaryA.get(x - 1), point1.add(planeNormal));
                                    if (!plane.whichSide(verticesA.get(i)).equals(plane.whichSide(point1.add(point2).divide(2)))) {
                                        verticesA.get(i).set(point1);
                                        break;

                                    }
                                    plane.setPlanePoints(point2, boundaryA.get((x + 2) % boundaryA.size()), point2.add(planeNormal));

                                    if (!plane.whichSide(verticesA.get(i)).equals(plane.whichSide(point1.add(point2).divide(2)))) {
                                        verticesA.get(i).set(point2);
                                        break;
                                    }
                                    break;
                                }
                            }
                        }
                    } 
                }
            }
            planeNormal = verticesB.get(0).subtract(verticesB.get(1)).cross(verticesB.get(2).subtract(verticesB.get(1)));
            planeBot.setPlanePoints(verticesB.get(0), verticesB.get(1), verticesB.get(1).add(planeNormal));
            planeTop.setPlanePoints(verticesB.get(2), verticesB.get(1), verticesB.get(1).add(planeNormal));
            for (int i = 3; i < verticesB.size(); i++) {
                if (FastMath.abs(planeBot.pseudoDistance(verticesB.get(i))) < FastMath.FLT_EPSILON || FastMath.abs(planeTop.pseudoDistance(verticesB.get(i))) < FastMath.FLT_EPSILON) {
                    //Do Nothing
                } else {
                    if (planeBot.whichSide(verticesB.get(i)).equals(planeBot.whichSide(verticesB.get(2)))
                            && planeTop.whichSide(verticesB.get(i)).equals(planeTop.whichSide(verticesB.get(0)))) {
                        for (int x = 2; x < boundaryB.size(); x++) {
                            Vector3f point1 = boundaryB.get(x);
                            Vector3f point2;
                            if (x == boundaryB.size() - 1) {
                                point2 = boundaryB.get(0);
                            } else {
                                point2 = boundaryB.get(x + 1);
                            }
                            Float angle1 = point1.subtract(verticesB.get(1)).normalize().angleBetween(verticesB.get(0).subtract(verticesB.get(1)).normalize());
                            Float angle2 = point2.subtract(verticesB.get(1)).normalize().angleBetween(verticesB.get(0).subtract(verticesB.get(1)).normalize());
                            Float targetAngle = verticesB.get(i).subtract(verticesB.get(1)).normalize().angleBetween(verticesB.get(0).subtract(verticesB.get(1)).normalize());
                            if ((angle1 > targetAngle && angle2 < targetAngle)) {
                                Plane plane = new Plane();
                                plane.setPlanePoints(point1, point2, point1.add(planeNormal));
                                if (plane.whichSide(verticesB.get(i)).equals(plane.whichSide(verticesB.get(1)))) {
                                    break;
                                } else {
                                    verticesB.get(i).set(plane.getClosestPoint(verticesB.get(i)));

                                    plane.setPlanePoints(point1, boundaryB.get(x - 1), point1.add(planeNormal));
                                    if (!plane.whichSide(verticesB.get(i)).equals(plane.whichSide(point1.add(point2).divide(2)))) {
                                        verticesB.get(i).set(point1);
                                        break;

                                    }
                                    plane.setPlanePoints(point2, boundaryB.get((x + 2) % boundaryB.size()), point2.add(planeNormal));

                                    if (!plane.whichSide(verticesB.get(i)).equals(plane.whichSide(point1.add(point2).divide(2)))) {
                                        verticesB.get(i).set(point2);
                                        break;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

        }

    }


    /**
     * updates the safety area
     */
    private void updateBoundaries() {
        ArrayList<ArrayList<Vector3f>> results = app.popUpBook.getBoundarys(patchA.geometry, patchB.geometry,
                verticesA.get(0), verticesA.get(1), verticesB.get(0), verticesB.get(1),
                verticesA.get(2), verticesA.get(1), verticesA.get(2), verticesA.get(1),
                "D1Joint");

        if (results != null) {
            boundaryA = results.get(0);
            boundaryB = results.get(1);
            if (boundaryAGeom == null) {
                boundaryAGeom = new Geometry(mode, Util.makeMesh(boundaryA.toArray(new Vector3f[boundaryA.size()])));
                boundaryBGeom = new Geometry(mode, Util.makeMesh(boundaryB.toArray(new Vector3f[boundaryB.size()])));
                Material allowed = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                allowed.setColor("Color", new ColorRGBA(0, 1, 0, 0.5f));
                allowed.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                allowed.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

                boundaryAGeom.setQueueBucket(Bucket.Transparent);
                boundaryBGeom.setQueueBucket(Bucket.Transparent);
                boundaryAGeom.setMaterial(allowed);
                boundaryBGeom.setMaterial(allowed.clone());
                tempNode.attachChild(boundaryAGeom);
                tempNode.attachChild(boundaryBGeom);

            } else {
                boundaryAGeom.setMesh(Util.makeMesh(boundaryA.toArray(new Vector3f[boundaryA.size()])));
                boundaryBGeom.setMesh(Util.makeMesh(boundaryB.toArray(new Vector3f[boundaryB.size()])));
            }
        }

    }

    /**
     * add line between two points
     * @param from
     * @param to 
     */
    private void addLine(Vector3f from, Vector3f to) {
        Geometry line = new Geometry("Line", new Cylinder());
        line.setMaterial(lineMaterial);
        updateLine(line, to, from);
        lineVecticesMap.put(line, new Vector3f[]{from, to});
        frameNode.attachChild(line);
    }

    /**
     * updates the state of the line
     * @param line geometry of the line
     * @param vertexA line starting point
     * @param vertexB line ending point
     */
    private void updateLine(Geometry line, Vector3f vertexA, Vector3f vertexB) {
        if (line.getName().equals("Line")) {
            line.setLocalTranslation(vertexA.add(vertexB).divide(2f));
            ((Cylinder) line.getMesh()).updateGeometry(5, 3, lineRadius, lineRadius, vertexA.distance(vertexB), false, false);
            line.lookAt(vertexA, new Vector3f(0, 1, 0));
        }
    }

    /**
     * Adds Dot geometry to represent vertex
     * @param dotLocation  vertex location
     */
    private void addDot(Vector3f dotLocation) {
        if (!dotVecticesMap.values().contains(dotLocation)) {
            Sphere sphere = new Sphere(8, 8, sphereRadius);
            Geometry dot = new Geometry("Dot", sphere);
            dot.setMaterial(dotMaterial.clone());
            dot.setLocalTranslation(dotLocation);
            frameNode.attachChild(dot);
            dotVecticesMap.put(dot, dotLocation);
        }
    }

    /**
     * remove listener and tempNode when app state is disabled
     */
    @Override
    protected void onDisable() {
        inputManager.removeListener(d1SBasicInput);
        app.getRootNode().detachChild(tempNode);
    }

    /**
     * update the visual of the patches when the data for the patches changed 
     */
    private void updateGraphics() {
        for (HashMap.Entry pair : dotVecticesMap.entrySet()) {
            ((Geometry) pair.getKey()).setLocalTranslation((Vector3f) pair.getValue());
        }
        for (HashMap.Entry pair : lineVecticesMap.entrySet()) {
            Vector3f[] points = (Vector3f[]) pair.getValue();
            updateLine((Geometry) pair.getKey(), points[0], points[1]);
        }
    }

    /**
     * get the Dot geometry at a location
     * @param point
     * @return 
     */
    private Geometry getDot(Vector3f point) {
        for (HashMap.Entry<Geometry, Vector3f> pair : dotVecticesMap.entrySet()) {
            if (pair.getValue().equals(point)) {
                return pair.getKey();
            }
        }
        return null;
    }
}
