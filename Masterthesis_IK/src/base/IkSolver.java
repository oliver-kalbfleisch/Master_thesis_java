package base;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
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
import au.edu.federation.caliko.visualisation.FabrikConstraint3D;
import au.edu.federation.caliko.visualisation.FabrikLine3D;
import au.edu.federation.caliko.visualisation.Point3D;
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
	private Mat4f transfMat = new Mat4f();
	private Vec3f[] targets;
	static final int WIDTH = 640;
	static final int HEIGHT = 480; // Window width and height
	private BroadcastingClient bcPublisher;
	private HandData handData = new HandData();

	private long lastFrame;
	private long lastFPS;
	private int fps;

	private long window; // Window handle
	// 2D projection matrix. Params: Left, Right, Top, Bottom, Near, Far
	Mat4f mvpMatrix = Mat4f.createPerspectiveProjectionMatrix(60.0f, (float) WIDTH / (float) HEIGHT, 1.0f, 10000.0f);
	// Initializte StuctureModel For hand

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

	public void run() throws SocketException { // Create our chain

		// TODO FIND CORRECT SCALE FOR MEASUREMENTS
		// Depth offset corresponds to distance from camera to base of tracking volume
		// in cm
		float depthOffset = 00.0f;
		float lenghtMultplier = 1.2f;
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
		Vec3f thumbConEnd = new Vec3f(-15.5f, 8.0f * lenghtMultplier, 0.0f);
		FabrikBone3D thumbConnect = new FabrikBone3D(thumbConStart, thumbConEnd);
		FabrikChain3D thumbConChain = new FabrikChain3D();
		thumbConnect.setColour(baseColor);
		thumbConnect.setBallJointConstraintDegs(0);
		thumbConChain.addBone(thumbConnect);
		thumbConChain.setFixedBaseMode(true);
		handStructureModel.connectChain(thumbConChain, 0, 0, BoneConnectionPoint.END);

		Vec3f thumbBaseStart = new Vec3f();
		Vec3f thumbBaseEnd = thumbBaseStart.plus(boneDirection.times(18.0f * lenghtMultplier));
		// thumb MP
		FabrikBone3D thumbBase = new FabrikBone3D(thumbBaseStart, thumbBaseEnd);
		cols[0] = new Colour4f(Utils.RED);
		thumbBase.setColour(cols[0].lighten(0.4f));
		thumb.addBone(thumbBase);
		// thumb PIP
		thumb.addConsecutiveHingedBone(boneDirection, 14.50f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[0].darken(0.4f));
		fingers[0] = thumb;
		thumb.setFixedBaseMode(true);
		// thumb.setHingeBaseboneConstraint(BaseboneConstraintType3D.LOCAL_HINGE,
		// Y_AXIS, 0, 95, X_AXIS);
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
		indexF.addConsecutiveHingedBone(boneDirection, 13.5f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[1].lighten(0.4f));
		// index DIP
		indexF.addConsecutiveHingedBone(boneDirection, 11.75f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[1].darken(0.4f));
		fingers[1] = indexF;
		indexF.setFixedBaseMode(true);
		// indexF.setHingeBaseboneConstraint(BaseboneConstraintType3D.LOCAL_HINGE,
		// Y_AXIS, 5, 90, X_AXIS);
		handStructureModel.connectChain(indexF, 3, 0, BoneConnectionPoint.END);

		// middle Finger------------------------------------------------------------
		Vec3f middleFConStart = handBaseBoneEnd;
		Vec3f middelfFConEnd = new Vec3f(0.0f, 10.0f * lenghtMultplier, 0.0f);
		Vec3f middleFBaseStart = new Vec3f();
		Vec3f middleFBaseEnd = middleFBaseStart.plus(boneDirection.times(5.0f * lenghtMultplier));
		FabrikBone3D middleFConnect = new FabrikBone3D(middleFConStart, middelfFConEnd);
		middleFConnect.setColour(baseColor);
		FabrikChain3D middleFConChain = new FabrikChain3D();
		middleFConChain.addBone(middleFConnect);
		handStructureModel.addChain(middleFConChain);
		// middlef MP
		FabrikBone3D middleFBase = new FabrikBone3D(middleFBaseStart, middleFBaseEnd);
		cols[2] = new Colour4f(Utils.YELLOW);
		middleFBase.setColour(cols[2].lighten(0.4f));
		middleF.addBone(middleFBase);
		// middle PIP
		middleF.addConsecutiveHingedBone(boneDirection, 30f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[2].darken(0.4f));
		// middle DIP
		middleF.addConsecutiveHingedBone(boneDirection, 20f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[2].lighten(0.4f));

		fingers[2] = middleF;
		// middleF.setHingeBaseboneConstraint(BaseboneConstraintType3D.LOCAL_HINGE,
		// X_AXIS, 45, 95, Y_AXIS);
		middleF.setFixedBaseMode(true);
		handStructureModel.connectChain(middleF, 5, 0, BoneConnectionPoint.END);

		// ring finger------------------------------------------------------------
		Vec3f ringFConnStart = handBaseBoneEnd;
		Vec3f ringFConnEnd = new Vec3f(7.0f, 10.0f * lenghtMultplier, 0.0f);
		Vec3f ringFBaseStart = new Vec3f();
		Vec3f ringFBaseEnd = ringFBaseStart.plus(boneDirection.times(24.0f * lenghtMultplier));
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
		// ringF.setGlobalHingedBasebone(X_AXIS, 90, 0, Y_AXIS);
		// ring PIP
		ringF.addConsecutiveHingedBone(boneDirection, 15.0f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[3].lighten(0.4f));
		// ring DIP
		ringF.addConsecutiveHingedBone(boneDirection, 12.25f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[3].darken(0.4f));
		fingers[3] = ringF;
		// ringF.setHingeBaseboneConstraint(BaseboneConstraintType3D.LOCAL_HINGE,
		// X_AXIS, 45, 95, Y_AXIS);
		ringF.setFixedBaseMode(true);
		handStructureModel.connectChain(ringF, 7, 0, BoneConnectionPoint.END);

		// little Finger
		Vec3f littleFConStart = handBaseBoneEnd;
		Vec3f littlefConEnd = new Vec3f(10.0f, 10.0f * lenghtMultplier, 0.0f);
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
		littleF.addConsecutiveHingedBone(boneDirection, 10.25f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[4].lighten(0.4f));
		// little DIP
		littleF.addConsecutiveHingedBone(boneDirection, 10.25f * lenghtMultplier, JointType.LOCAL_HINGE, X_AXIS, 0, 95,
				Y_AXIS, cols[4].darken(0.4f));
		fingers[4] = littleF;
		// littleF.setHingeBaseboneConstraint(BaseboneConstraintType3D.LOCAL_HINGE,
		// X_AXIS, 45, 95, Y_AXIS);
		littleF.setFixedBaseMode(true);

		handStructureModel.connectChain(littleF, 9, 0, BoneConnectionPoint.END);

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

	private void init() {

		// position Camera
		transfMat.setIdentity();
		Mat4f rotMat = new Mat4f();
		rotMat.setIdentity();
		rotMat = rotMat.rotateAboutLocalAxisDegs(45, Y_AXIS);
		transfMat = transfMat.translate(10.0f, -30.0f, -250.0f);
		mvpMatrix = mvpMatrix.times(transfMat);

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

		// send Start Command to raspberrys
		bcPublisher = new BroadcastingClient();
		// TODO Add Button for System Start
		try {
			// Use loacal network broadcast address!!
			bcPublisher.broadcast("Hello", InetAddress.getByName(BROADCASTIP));
		} catch (Exception e) {
			System.out.println("Could not send start signal to network, check Connection");
			e.printStackTrace();
		}
	}

	public void updateStructurePos(FabrikStructure3D structure, Vec3f handBasePos) {
		Vec3f delta = handBasePos;
		Vec3f test = handBaseChain.getBaseLocation();
		Vec3f.subtract(handBasePos, test);
		int numChains = structure.getNumChains();
		for (int i = 0; i < numChains; i++) {
			FabrikChain3D chain = structure.getChain(i);
			for (int k = 0; k < chain.getNumBones(); k++) {
				FabrikBone3D bone = chain.getBone(k);
				Vec3f sl = bone.getStartLocation();
				Vec3f el = bone.getEndLocation();
				Vec3f.add(sl, delta);
				bone.setStartLocation(sl);
				Vec3f.add(el, delta);
				bone.setEndLocation(el);
			}
		}
	}

	public void update() throws SocketException {

		KeyboardHandler.isKeyDown(GLFW_KEY_ESCAPE);
		if (KeyboardHandler.isKeyDown(GLFW_KEY_ESCAPE)) {
			glfwSetWindowShouldClose(window, true);
			System.exit(0);
		}
		try {
			// Retrieve current Data
			Vec2f[] rVec = calc.getDataset(0);
			Vec2f[] lVec = calc.getDataset(1);
			// Calculate Z distance

			for (int i = 0; i < rVec.length; i++) {

				calculateDepthData(rVec, lVec, i);
			}
			updateStructurePos(handStructureModel, handData.getCurrentFingerPos()[5]);

		} catch (Exception e) {

			System.out.println("No tracking data available");
		}

		if (KeyboardHandler.isKeyDown(GLFW_KEY_Q)) {

			Mat4f rotMat = new Mat4f();
			rotMat.setIdentity();
			rotMat = rotMat.rotateAboutLocalAxisDegs(0.1f, Y_AXIS);
			mvpMatrix = mvpMatrix.times(rotMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_E)) {
			Mat4f rotMat = new Mat4f();
			rotMat.setIdentity();
			rotMat = rotMat.rotateAboutLocalAxisDegs(-0.1f, Y_AXIS);
			mvpMatrix = mvpMatrix.times(rotMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_W)) {

			transfMat.setIdentity();
			transfMat = transfMat.translate(0.0f, 0.0f, 0.10f);
			mvpMatrix = mvpMatrix.times(transfMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_S)) {

			transfMat.setIdentity();
			transfMat = transfMat.translate(0.0f, 0.0f, -0.10f);
			mvpMatrix = mvpMatrix.times(transfMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_A)) {
			transfMat.setIdentity();
			transfMat = transfMat.translate(0.1f, 0.0f, 0.0f);
			mvpMatrix = mvpMatrix.times(transfMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_D)) {

			transfMat.setIdentity();
			transfMat = transfMat.translate(-0.10f, 0.0f, 0.0f);
			mvpMatrix = mvpMatrix.times(transfMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_R)) {

			transfMat.setIdentity();
			transfMat = transfMat.translate(0.0f, -0.10f, 0.0f);
			mvpMatrix = mvpMatrix.times(transfMat);
		}
		if (KeyboardHandler.isKeyDown(GLFW_KEY_F)) {

			transfMat.setIdentity();
			transfMat = transfMat.translate(0.0f, 0.10f, 0.0f);
			mvpMatrix = mvpMatrix.times(transfMat);
		}
	}

	/**
	 * @param rightSdeData
	 * @param leftSideData
	 * @param targetNumber
	 */
	private void calculateDepthData(Vec2f[] rightSdeData, Vec2f[] leftSideData, int targetNumber) {
		try {
			float zDist = calc.calculateZDistance(0.075f, 62.2f, 640, rightSdeData[targetNumber].x,
					leftSideData[targetNumber].x, targets[targetNumber].z);
			targets[targetNumber] = new Vec3f(rightSdeData[targetNumber].x, rightSdeData[targetNumber].y, zDist);
			handData.updateCurrentFingerPos(targetNumber,
					new Vec3f(rightSdeData[targetNumber].x, rightSdeData[targetNumber].y, zDist));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loop() throws SocketException {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();
		// Set the clear color
		glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.

		// Vec3f[] targets = handData.getCurrentFingerPos();
		FabrikConstraint3D constraint = new FabrikConstraint3D();
		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear buffers
			// Draw refernec coordinate system
			FabrikBone3D xbone = new FabrikBone3D(new Vec3f(0.0f, 0.0f, 0.0f), new Vec3f(1000.0f, 0.0f, 0.0f));
			FabrikLine3D.draw(xbone, Utils.RED, 1.0f, mvpMatrix);
			FabrikBone3D ybone = new FabrikBone3D(new Vec3f(0.0f, 0.0f, 0.0f), new Vec3f(0.0f, 1000.0f, 0.0f));
			FabrikLine3D.draw(ybone, Utils.BLUE, 1.0f, mvpMatrix);
			FabrikBone3D zbone = new FabrikBone3D(new Vec3f(0.0f, 0.0f, 0.0f), new Vec3f(0.0f, 0.0f, -1000.0f));
			FabrikLine3D.draw(zbone, Utils.GREEN, 1.0f, mvpMatrix);
			//
			for (int i = 0; i < fingers.length; i++) {
				drawTargetAndSolve(i);

			}
			FabrikLine3D.draw(handStructureModel, 3.0f, mvpMatrix);
			glfwSwapBuffers(window); // Swap colour buf.

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
			targetPoint.draw(handData.getCurrentFingerPos()[i], Utils.MAGENTA, 10.0f, mvpMatrix);
			handStructureModel.getChain(2 + (i * 2)).solveForTarget(handData.getCurrentFingerPos()[i]);
		} catch (Exception e) {
			System.out.println("not solved");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws SocketException {
		new IkSolver().run();
	}

	public int getDelta() {
		long time = getTime();
		int delta = (int) (time - lastFrame);
		lastFrame = time;

		return delta;
	}

	public long getTime() {
		return System.nanoTime();
	}

	public void updateFPS() {
		if (getTime() - lastFPS > 1000) {
			System.out.println(fps);
			fps = 0;
			lastFPS += 1000;
		}
		fps++;
	}
}
