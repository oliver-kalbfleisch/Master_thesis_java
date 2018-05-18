package base;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_K;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_L;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.IntBuffer;
import java.util.concurrent.TimeUnit;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import Input.KeyboardHandler;
import au.edu.federation.caliko.BoneConnectionPoint;
import au.edu.federation.caliko.FabrikBone3D;
import au.edu.federation.caliko.FabrikChain3D;
import au.edu.federation.caliko.FabrikChain3D.BaseboneConstraintType3D;
import au.edu.federation.caliko.FabrikJoint3D.JointType;
import au.edu.federation.caliko.FabrikStructure3D;
import au.edu.federation.caliko.*;
import au.edu.federation.caliko.visualisation.FabrikConstraint3D;
import au.edu.federation.caliko.visualisation.FabrikLine3D;
import au.edu.federation.caliko.visualisation.FabrikModel3D;
import au.edu.federation.caliko.visualisation.Model;
import au.edu.federation.caliko.visualisation.Point3D;
import au.edu.federation.caliko.visualisation.*;
import au.edu.federation.utils.Colour4f;
import au.edu.federation.utils.Mat4f;
import au.edu.federation.utils.Utils;
import au.edu.federation.utils.Vec2f;
import au.edu.federation.utils.Vec3f;

public class IkSolver {

	private static final String BROADCASTIP = "192.168.1.255";
	// Define cardinal axes
	static final Vec3f X_AXIS = new Vec3f(1.0f, 0.0f, 0.0f);
	static final Vec3f Y_AXIS = new Vec3f(0.0f, 1.0f, 0.0f);
	static final Vec3f Z_AXIS = new Vec3f(0.0f, 0.0f, 1.0f);
	// This prevents our window from crashing later on.
	private GLFWErrorCallback errorCB;
	private GLFWKeyCallback keyCB;
	private GLFWCursorPosCallback cpCallback;
	private FabrikChain3D[] fingers = new FabrikChain3D[5];
	private static Colour4f[] cols = new Colour4f[5];
	private Vec3f[] targets;
	static final int WIDTH = 640;
	static final int HEIGHT = 480; // Window width and height
	private HandData handData = new HandData(6);
	private Axis axis;
	private Axis structureAxis;
	private FabrikModel3D model;
	private long window; // Window handle
	private Camera camera;
	/** time at last frame */
	long lastFrame;

	/** frames per second */
	int fps;
	/** last fps time */
	long lastFPS;

	private Mat4f projectionMatrix = Mat4f.createPerspectiveProjectionMatrix(60.0f, (float) WIDTH / (float) HEIGHT,
			1.0f, 10000.0f);
	private Mat4f projectionViewMatrix;
	private Mat4f viewMatrix = new Mat4f();
	FabrikStructure3D handStructureModel = new FabrikStructure3D();
	private Vec3f handBasePos;
	// Initialize fingers
	// Thumb
	FabrikChain3D thumb = new FabrikChain3D();
	FabrikChain3D thumbConnectorChain = new FabrikChain3D();
	// index finger
	FabrikChain3D indexF = new FabrikChain3D();
	Colour4f blue = new Colour4f(Utils.BLUE);
	// middle Finger
	FabrikChain3D middleF = new FabrikChain3D();
	Colour4f yellow = new Colour4f(Utils.YELLOW);
	// ring Finger
	FabrikChain3D ringF = new FabrikChain3D();
	Colour4f cyan = new Colour4f(Utils.CYAN);
	// little Finger
	FabrikChain3D littleF = new FabrikChain3D();
	Colour4f magenta = new Colour4f(Utils.MAGENTA);
	FabrikBone3D handBase;
	FabrikChain3D handBaseChain = new FabrikChain3D();

	// STEREO STUFF
	StereoCalculator calc;

	public void run() throws SocketException, InterruptedException { // Create our chain

		try {
			init();
			loop();
			// Release window and window callbacks
			glfwDestroyWindow(window);
			keyCB.free();
			cpCallback.free();
		} finally {
			// Terminate GLFW and release the GLFWErrorCallback
			glfwTerminate();
			errorCB.free();
		}
	}

	public void handleCameraMovement(int key, int action) {
		camera.handleKeypress(key, action);
	}

	private void init() throws InterruptedException {
		// TODO FIND CORRECT SCALE FOR MEASUREMENTS
		// Depth offset corresponds to distance from camera to base of tracking volume
		// in cm
		float depthOffset = 00.0f;
		float lenghtMultplier = 1.0f;
		Vec3f boneDirection = new Vec3f(0.0f, 1.0f, .0f);

		handBasePos = new Vec3f(0.0f, 0.0f, depthOffset);
		Vec3f handBaseBoneStart = handBasePos;
		Vec3f handBaseBoneEnd = new Vec3f(handBasePos.x, handBasePos.y + 0.01f, handBasePos.z);
		handBase = new FabrikBone3D(handBaseBoneStart, handBaseBoneEnd);
		handBaseChain.addBone(handBase);
		handBaseChain.setFixedBaseMode(true);
		handStructureModel.addChain(handBaseChain);
		Colour4f baseColor = new Colour4f(Utils.BLACK);
		handBase.setColour(baseColor.lighten(0.4f));
		handBaseChain.setFixedBaseMode(true);
		// Thumb------------------------------------------------------------
		Vec3f thumbConStart = handBaseBoneEnd;
		Vec3f thumbConEnd = new Vec3f(-15.5f, 0.0f * lenghtMultplier, 0.0f);
		FabrikBone3D thumbConnect = new FabrikBone3D(thumbConStart, thumbConEnd);
		FabrikChain3D thumbConChain = new FabrikChain3D();
		thumbConnect.setColour(baseColor);
		thumbConnect.setBallJointConstraintDegs(0);
		thumbConChain.addBone(thumbConnect);
		thumbConChain.setFixedBaseMode(true);
		handStructureModel.connectChain(thumbConChain, 0, 0, BoneConnectionPoint.END);

		Vec3f thumbBaseStart = new Vec3f();
		Vec3f thumbBaseEnd = thumbBaseStart.plus(boneDirection.times(30.0f * lenghtMultplier));
		// thumb MP
		FabrikBone3D thumbBase = new FabrikBone3D(thumbBaseStart, thumbBaseEnd);
		cols[0] = new Colour4f(Utils.RED);
		thumbBase.setColour(cols[0].lighten(0.4f));
		thumb.addBone(thumbBase);
		// thumb PIP
		thumb.addConsecutiveHingedBone(boneDirection, 30.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[0].darken(0.4f));
		fingers[0] = thumb;
		thumb.setFixedBaseMode(true);
		thumb.setRotorBaseboneConstraint(BaseboneConstraintType3D.LOCAL_ROTOR, Y_AXIS, 35.0f);
		handStructureModel.connectChain(thumb, 1, 0, BoneConnectionPoint.END);
		/*
		 * 
		 */
		// Index Finger----------------------------------------------------
		Vec3f indexConStart = handBaseBoneEnd;
		Vec3f indexConEnd = new Vec3f(-9.0f, 10.0f * lenghtMultplier, 0.0f);
		Vec3f indexFBaseStart = new Vec3f();
		Vec3f indexFBaseEnd = indexFBaseStart.plus(boneDirection.times(25.0f * lenghtMultplier));
		FabrikBone3D indexConnect = new FabrikBone3D(indexConStart, indexConEnd);
		indexConnect.setColour(baseColor);
		FabrikChain3D indexConChain = new FabrikChain3D();
		indexConChain.addBone(indexConnect);
		indexConChain.setFixedBaseMode(true);
		handStructureModel.addChain(indexConChain);
		//
		// index MP
		FabrikBone3D indexFBase = new FabrikBone3D(indexFBaseStart, indexFBaseEnd);
		cols[1] = new Colour4f(Utils.BLUE);
		indexFBase.setColour(cols[1].darken(0.4f));
		indexF.addBone(indexFBase);
		// index PIP
		indexF.addConsecutiveHingedBone(boneDirection, 23.5f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 110,
				Y_AXIS, cols[1].lighten(0.4f));
		// index DIP
		indexF.addConsecutiveHingedBone(boneDirection, 22.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 90,
				Y_AXIS, cols[1].darken(0.4f));
		fingers[1] = indexF;
		indexF.setFixedBaseMode(true);
		//indexF.setHingeBaseboneConstraint(BaseboneConstraintType3D.GLOBAL_HINGE, X_AXIS, 90, 5, Y_AXIS);
		indexF.setRotorBaseboneConstraint(BaseboneConstraintType3D.LOCAL_ROTOR, Y_AXIS, 15.0f);
		handStructureModel.connectChain(indexF, 3, 0, BoneConnectionPoint.END);

		// middle Finger------------------------------------------------------------
		Vec3f middleFConStart = handBaseBoneEnd;
		Vec3f middelfFConEnd = new Vec3f(0.0f, 10.0f * lenghtMultplier, 0.0f);
		Vec3f middleFBaseStart = new Vec3f();
		Vec3f middleFBaseEnd = middleFBaseStart.plus(boneDirection.times(27.0f * lenghtMultplier));
		FabrikBone3D middleFConnect = new FabrikBone3D(middleFConStart, middelfFConEnd);
		middleFConnect.setColour(baseColor);
		FabrikChain3D middleFConChain = new FabrikChain3D();
		middleFConChain.addBone(middleFConnect);
		handStructureModel.addChain(middleFConChain);
		// middlef MP
		FabrikBone3D middleFBase = new FabrikBone3D(middleFBaseStart, middleFBaseEnd);
		cols[2] = new Colour4f(Utils.MID_BLUE);
		middleFBase.setColour(cols[2].lighten(0.4f));
		middleF.addBone(middleFBase);
		// middle PIP
		middleF.addConsecutiveHingedBone(boneDirection, 23.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[2].darken(0.4f));
		// middle DIP
		middleF.addConsecutiveHingedBone(boneDirection, 25f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[2].lighten(0.4f));

		fingers[2] = middleF;
		//middleF.setHingeBaseboneConstraint(BaseboneConstraintType3D.GLOBAL_HINGE, X_AXIS, 90, 5, Y_AXIS);
		middleF.setRotorBaseboneConstraint(BaseboneConstraintType3D.LOCAL_ROTOR, Y_AXIS, 15.0f);
		middleF.setFixedBaseMode(true);
		handStructureModel.connectChain(middleF, 5, 0, BoneConnectionPoint.END);

		// ring finger------------------------------------------------------------
		Vec3f ringFConnStart = handBaseBoneEnd;
		Vec3f ringFConnEnd = new Vec3f(7.0f, 10.0f * lenghtMultplier, 0.0f);
		Vec3f ringFBaseStart = new Vec3f();
		Vec3f ringFBaseEnd = ringFBaseStart.plus(boneDirection.times(25.0f * lenghtMultplier));
		FabrikBone3D ringFConnect = new FabrikBone3D(ringFConnStart, ringFConnEnd);
		ringFConnect.setColour(baseColor);
		FabrikChain3D ringFConChain = new FabrikChain3D();
		ringFConChain.addBone(ringFConnect);
		handStructureModel.addChain(ringFConChain);
		// ring MP
		FabrikBone3D ringFBase = new FabrikBone3D(ringFBaseStart, ringFBaseEnd);
		cols[3] = new Colour4f(Utils.CYAN);
		ringFBase.setColour(cols[3].darken(0.8f));
		ringF.addBone(ringFBase);
		// ring PIP
		ringF.addConsecutiveHingedBone(boneDirection, 22.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[3].lighten(0.4f));
		// ring DIP
		ringF.addConsecutiveHingedBone(boneDirection, 23.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[3].darken(0.4f));
		fingers[3] = ringF;
		//ringF.setHingeBaseboneConstraint(BaseboneConstraintType3D.GLOBAL_HINGE, X_AXIS, 90, 5, Y_AXIS);
		ringF.setRotorBaseboneConstraint(BaseboneConstraintType3D.LOCAL_ROTOR, Y_AXIS, 15.0f);
		ringF.setFixedBaseMode(true);
		handStructureModel.connectChain(ringF, 7, 0, BoneConnectionPoint.END);

		// little Finger
		Vec3f littleFConStart = handBaseBoneEnd;
		Vec3f littlefConEnd = new Vec3f(15.0f, 8.0f * lenghtMultplier, 0.0f);
		Vec3f littleFBaseStart = new Vec3f();
		Vec3f littleFBaseEnd = littleFBaseStart.plus(boneDirection.times(19.25f * lenghtMultplier));
		FabrikBone3D littleFConnect = new FabrikBone3D(littleFConStart, littlefConEnd);
		littleFConnect.setColour(baseColor);
		FabrikChain3D littleFConChain = new FabrikChain3D();
		littleFConChain.addBone(littleFConnect);
		handStructureModel.addChain(littleFConChain);

		// little MP
		FabrikBone3D littleFBase = new FabrikBone3D(littleFBaseStart, littleFBaseEnd);
		cols[4] = new Colour4f(Utils.MAGENTA);
		littleFBase.setColour(cols[4].darken(0.4f));
		littleF.addBone(littleFBase);

		// little PIP
		littleF.addConsecutiveHingedBone(boneDirection, 16.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[4].lighten(0.4f));
		// little DIP
		littleF.addConsecutiveHingedBone(boneDirection, 22.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[4].darken(0.4f));
		fingers[4] = littleF;
		//littleF.setHingeBaseboneConstraint(BaseboneConstraintType3D.GLOBAL_HINGE, X_AXIS, 90, 5, Y_AXIS);
		littleF.setRotorBaseboneConstraint(BaseboneConstraintType3D.LOCAL_ROTOR, Y_AXIS, 15.0f);
		littleF.setFixedBaseMode(true);

		handStructureModel.connectChain(littleF, 9, 0, BoneConnectionPoint.END);

		viewMatrix.setIdentity();
		Mat4f rotMat = new Mat4f();
		rotMat.setIdentity();
		rotMat = rotMat.rotateAboutLocalAxisDegs(45, Y_AXIS);
		viewMatrix = viewMatrix.translate(10.0f, -30.0f, -250.0f);
		projectionViewMatrix = projectionMatrix.times(viewMatrix);
		// Init of UDP listening and data Calculation in separate thread
		int numColors = 6;
		calc = new StereoCalculator(numColors);
		// // // Step one init UPD Listeners for both cameras
		try {
			calc.createUDPListener(8888, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			calc.createUDPListener(9999, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		TimeUnit.SECONDS.sleep(2);
		// Init targets
		targets = new Vec3f[numColors];
		for (int i = 0; i < targets.length; i++) {
			targets[i] = new Vec3f(0, 0, 0);
		}

		// Setup an error callback. The default implementation // will print the error
		// message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();
		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");
		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
		// Create the window
		window = glfwCreateWindow(WIDTH, HEIGHT, "IkSolver Visual Demo", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback
		glfwSetKeyCallback(window, keyCB = new KeyboardHandler());
		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically
			// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);
		// Make the window visible
		glfwShowWindow(window);

		try {
			// Use loacal network broadcast address!!
			BroadcastingClient.broadcast("Hello", InetAddress.getByName(BROADCASTIP));
		} catch (Exception e) {
			System.out.println("Could not send start signal to network, check Connection");
			e.printStackTrace();
		}
		try {
			GL.createCapabilities();
			axis = new Axis(1000.0f, 2.0f);
			structureAxis = new Axis(10.0f, 5.0f);
			model = new FabrikModel3D("/pyramid.obj", 1.0f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void updateStructurePos(FabrikStructure3D structure, Vec3f handBasePos) {
		Vec3f delta = handBasePos;
		handData.setHandBasePos(new Vector3f(handBasePos.x, handBasePos.y, handBasePos.z));
		Vec3f test = handBaseChain.getBaseLocation();
		Vector3f temp = handData.getHandBasePos();
		temp.sub(new Vector3f(test.x, test.y, test.z));
		Vec3f.subtract(delta, test);

		// System.out.println(temp);
		int numChains = structure.getNumChains();
		for (int i = 0; i < numChains; i++) {
			FabrikChain3D chain = structure.getChain(i);
			for (int k = 0; k < chain.getNumBones(); k++) {
				FabrikBone3D bone = chain.getBone(k);
				Vec3f sl = bone.getStartLocation();
				Vec3f el = bone.getEndLocation();
				Vec3f.add(sl, delta);
				Vec3f.add(el, delta);
				bone.setStartLocation(sl);
				bone.setEndLocation(el);
			}
		}
	}

	public void update() throws SocketException {

		KeyboardHandler.isKeyDown(GLFW_KEY_ESCAPE);
		if (KeyboardHandler.isKeyDown(GLFW_KEY_ESCAPE)) {
			glfwSetWindowShouldClose(window, true);
			calc.udpthreadLeft.interrupt();
			calc.udpthreadRight.interrupt();
			System.exit(0);
		}
		try {
			// Retrieve current Data
			Vec2f[] rVec = calc.getDataset(0);
			Vec2f[] lVec = calc.getDataset(1);
			long er = calc.getEpochRight();
			long el = calc.getEpochLeft();
			long ed = el - er;
			// Calculate Z distance
			for (int i = 0; i < rVec.length; i++) {

				calculateDepthData(rVec, lVec, i);
			}
			try {
				updateStructurePos(handStructureModel, handData.getCurrentFingerPos()[5]);
			} catch (Exception e2) {
				// TODO: handle exception
			}


		} catch (Exception e) {
			// System.out.println("No tracking data available. Please check network
			// connections and System status before restart.");
			
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_Q)) {

			Mat4f rotMat = new Mat4f();
			rotMat.setIdentity();
			rotMat = rotMat.rotateAboutLocalAxisDegs(0.1f, Y_AXIS);
			projectionViewMatrix = projectionViewMatrix.times(rotMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_E)) {
			Mat4f rotMat = new Mat4f();
			rotMat.setIdentity();
			rotMat = rotMat.rotateAboutLocalAxisDegs(-0.1f, Y_AXIS);
			projectionViewMatrix = projectionViewMatrix.times(rotMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_W)) {

			viewMatrix.setIdentity();
			viewMatrix = viewMatrix.translate(0.0f, 0.0f, 0.10f);
			projectionViewMatrix = projectionViewMatrix.times(viewMatrix);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_S)) {

			viewMatrix.setIdentity();
			viewMatrix = viewMatrix.translate(0.0f, 0.0f, -0.10f);
			projectionViewMatrix = projectionViewMatrix.times(viewMatrix);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_A)) {
			viewMatrix.setIdentity();
			viewMatrix = viewMatrix.translate(0.1f, 0.0f, 0.0f);
			projectionViewMatrix = projectionViewMatrix.times(viewMatrix);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_D)) {

			viewMatrix.setIdentity();
			viewMatrix = viewMatrix.translate(-0.10f, 0.0f, 0.0f);
			projectionViewMatrix = projectionViewMatrix.times(viewMatrix);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_R)) {

			viewMatrix.setIdentity();
			viewMatrix = viewMatrix.translate(0.0f, -0.10f, 0.0f);
			projectionViewMatrix = projectionViewMatrix.times(viewMatrix);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_F)) {

			viewMatrix.setIdentity();
			viewMatrix = viewMatrix.translate(0.0f, 0.10f, 0.0f);
			projectionViewMatrix = projectionViewMatrix.times(viewMatrix);
		}
		// if (KeyboardHandler.isKeyDown(GLFW_KEY_K)) {
		// calc.mincutoffX-=0.001;
		// System.out.println(calc.mincutoffX);
		// }
		// if (KeyboardHandler.isKeyDown(GLFW_KEY_L)) {
		// calc.mincutoffX+=0.001;
		// System.out.println(calc.mincutoffX);
		// }
		//

	}

	/**
	 * @param rightSideData
	 * @param leftSideData
	 * @param targetNumber
	 */
	private void calculateDepthData(Vec2f[] rightSideData, Vec2f[] leftSideData, int targetNumber) {
		try {
			float zDist = calc.calculateZDistance(rightSideData[targetNumber].x,
					leftSideData[targetNumber].x);
			targets[targetNumber] = new Vec3f(rightSideData[targetNumber].x, rightSideData[targetNumber].y, zDist);
			handData.updateCurrentFingerPos(targetNumber,
					new Vec3f(rightSideData[targetNumber].x, rightSideData[targetNumber].y, zDist));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loop() throws SocketException {

		// Set the clear color
		glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

		// Run the rendering loop until the user has attempted to lose
		// the window or has pressed the ESCAPE key.

		FabrikConstraint3D constraint = new FabrikConstraint3D();
		int nbFrames = 0;
		double lastTime = glfwGetTime();
		while (!glfwWindowShouldClose(window)) {
			double currentTime = glfwGetTime();
			nbFrames++;
			if (currentTime - lastTime >= 1.0) { // If last prinf() was more than 1 sec ago
				// printf and reset timer
				// System.out.printf("%f ms/frame\n", 1000.0 / (double) (nbFrames));
				nbFrames = 0;
				lastTime += 1.0;
			}
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear buffers
			// Draw reference coordinate system
			 axis.draw(projectionViewMatrix);
			// Draw bone Coordinate systems
			// structureAxis.draw(handStructureModel, projectionViewMatrix, viewMatrix);
			for (int i = 0; i < fingers.length; i++) {
				 try {
					drawTargetAndSolve(i);
				} catch (Exception e) {
					continue;
				}
			}
			try {
				// model.drawStructure(handStructureModel, projectionViewMatrix, viewMatrix,
				// Utils.RED);
				FabrikLine3D.draw(handStructureModel, 1.0f, projectionViewMatrix);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(0);
			}
			// DEBUG
			constraint.draw(handStructureModel, 2.0f, projectionViewMatrix);
			glfwSwapBuffers(window); // Swap color buff.

			// Poll for window events. The key callback above will only be // invoked during
			// this call.
			glfwPollEvents();
			update();
		}
	}

	/**
	 * @param i
	 */
	private void drawTargetAndSolve(int i) {
		try {
			Point3D targetPoint = new Point3D();
			targetPoint.draw(handData.getCurrentFingerPos()[i], Utils.GREEN, 10.0f, projectionViewMatrix);
			handStructureModel.getChain(2 + (i * 2)).solveForTarget(handData.getCurrentFingerPos()[i]);
		} catch (Exception e) {
			System.out.println("not solved");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws SocketException, InterruptedException {
		new IkSolver().run();
	}

}
