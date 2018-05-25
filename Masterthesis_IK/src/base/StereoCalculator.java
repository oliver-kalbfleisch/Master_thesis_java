package base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import au.edu.federation.utils.Vec2f;

public class StereoCalculator {
	public int[] resZ;
	private int udpPortLeft;
	private int udpPortRight;
	private Vec2f[] coordinateSetsLeft;
	private Vec2f[] coordinateSetsRight;
	private  float handAngleLeft=0.0f;
	private  float handAngleRight=0.0f;
	private static final ReentrantReadWriteLock lockLeft = new ReentrantReadWriteLock(true);
	public volatile static String udpMessageLeft, udpMessageRight;
	private static final ReentrantReadWriteLock lockRight = new ReentrantReadWriteLock(true);
	private static final int CAMERA_IMAGE_WIDTH = 640;
	private static final int CAMERA_IMAGE_HEIGHT = 480;
	private long epochRight = 0;
	private long epochLeft = 0;
	private long prevEpochRight = 0;
	private long prevEpochLeft = 0;
	private int delta = 1500;
	private int zeroPlaneOffset = 90;
	private OneEuroFilter oeurFilterX;
	private OneEuroFilter oeurFilterY;
	private OneEuroFilter oeurFilterZ;
	protected Thread udpthreadLeft;
	protected Thread udpthreadRight;

	private double frequency = 60; // Hz
	private double mincutoffZ = 0.1;
	private double betaZ = 0.001;

	public double mincutoffY = 15; //0.05
	private double betaY = 0.001; //0.08

	public double mincutoffX = 30;
	private double betaX = 0;

	public StereoCalculator(int numElements) {
		this.resZ = new int[numElements];
		this.udpPortLeft = 8888;
		this.udpPortRight = 9999;
		this.coordinateSetsLeft = new Vec2f[numElements];
		this.coordinateSetsRight = new Vec2f[numElements];
		for (int i = 0; i < coordinateSetsLeft.length; i++) {
			coordinateSetsLeft[i] = new Vec2f(0, 0);
			coordinateSetsRight[i] = new Vec2f(0, 0);
			resZ[i] = 0;
		}
		try {
			oeurFilterZ = new OneEuroFilter(frequency, mincutoffZ, betaZ);
			oeurFilterY = new OneEuroFilter(frequency, mincutoffY, betaY);
			oeurFilterX = new OneEuroFilter(frequency, mincutoffX, betaX);
		} catch (Exception e) {
			System.out.println("filter exe");
			e.printStackTrace();
		}
	}

	class UDPProcessThreadLeft implements Runnable {
		private int udpListenPort;
		private boolean running = true;
		private DatagramSocket dsocket;
		private byte[] buffer;
		private DatagramPacket packet;
		private ReentrantReadWriteLock lock;

		public UDPProcessThreadLeft(int UDPPort, ReentrantReadWriteLock lock) throws SocketException {
			this.udpListenPort = UDPPort;
			this.dsocket = new DatagramSocket(this.udpListenPort);
			this.buffer = new byte[128];
			this.packet = new DatagramPacket(buffer, buffer.length);
			this.lock = lock;
		}

		@Override
		public void run() {
			try {

				while (running) {
					dsocket.receive(packet);
					String msg = new String(buffer, 0, packet.getLength());
					try {
						lock.writeLock().lock();
						udpMessageLeft = msg;
						epochLeft = Long.parseLong(msg.substring(msg.length() - 14, msg.length() - 1));

						udpMessageLeft = msg;

					} finally {
						lock.writeLock().unlock();
					}
					// Reset the length of the packet before reusing it.
					packet.setLength(buffer.length);
				}

			} catch (

			SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void stopListening() {
			this.running = false;
		}

		public void startListening() {
			this.running = true;
		}

	}

	class UDPProcessThreadRight implements Runnable {
		private int udpListenPort;
		private boolean running = true;
		private DatagramSocket dsocket;
		private byte[] buffer;
		private DatagramPacket packet;
		private ReentrantReadWriteLock lock;

		public UDPProcessThreadRight(int UDPPort, ReentrantReadWriteLock lock) throws SocketException {
			this.udpListenPort = UDPPort;
			this.dsocket = new DatagramSocket(this.udpListenPort);
			this.buffer = new byte[128];
			this.packet = new DatagramPacket(buffer, buffer.length);
			this.lock = lock;
		}

		@Override
		public void run() {
			try {

				while (running) {
					dsocket.receive(packet);
					String msg = new String(buffer, 0, packet.getLength());
					try {
						lock.writeLock().lock();
						udpMessageRight = msg;
						epochRight = Long.parseLong(msg.substring(msg.length() - 14, msg.length() - 1));
						//if (epochLeft - epochRight > -delta) {
							//System.out.println("rt:"+(epochLeft-epochRight));
							udpMessageRight = msg;
						//}

					} finally {
						lock.writeLock().unlock();
					}
					// Reset the length of the packet before reusing it.
					packet.setLength(buffer.length);
				}

			} catch (

			SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void stopListening() {
			this.running = false;
		}

		public void startListening() {
			this.running = true;
		}

	}

	/**
	 * Function creates a UDP Port to listen to for data from the camera of the
	 * specified side
	 * 
	 * @param port
	 *            Port on which to listen to UDP Packages
	 * @param cameraSide
	 *            Phyical position of the camera in the system (left=0,right=1)
	 * @throws IOException
	 */
	public void createUDPListener(int port, int cameraSide) throws IOException {
		switch (cameraSide) {
		case 0:

			udpthreadLeft = new Thread(new UDPProcessThreadLeft(port, lockLeft));
			udpthreadLeft.start();
			break;
		case 1:

			udpthreadRight = new Thread(new UDPProcessThreadRight(port, lockRight));
			udpthreadRight.start();
			break;
		default:
			System.out.println("invald camera selection parameter " + cameraSide);
			break;
		}
	}
	/**
	 * Function processes the incoming message parts to get the position information
	 * from the input String
	 * 
	 * @param parts
	 *            The split parts of the received UDP String message containing the
	 *            tracking data
	 * @param coordinateSet
	 *            The 2D vector into which the extracted data will be written
	 */
	private void processMessage(String[] parts, Vec2f[] coordinateSet, float angle) {

		for (int i = 0; i < parts.length - 2; i++) {
			String[] values = parts[i].split(",");
			// Extract value by extracting substrings at correct positions
			int xVal = 0;
			int yVal = 0;
			try {
				xVal = Integer.parseInt(values[0].substring(1));
				yVal = Integer.parseInt(values[1].substring(1, values[1].length() - 1));
				xVal = (int) clamp(0.0, 640.0, (double) (xVal));
				yVal = (int) clamp(0.0, 480.0, (double) (yVal));
			} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			float currentX = 0;
			try {
				currentX = (float) oeurFilterX.filter(-(xVal - (CAMERA_IMAGE_WIDTH / 2.0)));
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			float currentY = 0;
			try {
				currentY = (float) oeurFilterY.filter((yVal - (CAMERA_IMAGE_HEIGHT / 2.0)));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//float currentX = (float) -(xVal - (CAMERA_IMAGE_WIDTH / 2.0));
			//float currentY = (float) (yVal - (CAMERA_IMAGE_HEIGHT / 2.0));
			// Filter out jumps in tracking data
			try {
				coordinateSet[i].x = currentX;
				coordinateSet[i].y = currentY;
				
			} catch (ArrayIndexOutOfBoundsException ae) {

			}
		}
	}

	private float calculateCameraUpdateFrequency(int side) {
		switch (side) {
		// left
		case 0: {
			long deltaL = epochLeft - prevEpochLeft;
			return ((int) ((1.0f / deltaL) * 1000));
		}
		// right
		case 1: {
			long deltaR = epochRight - prevEpochRight;
			return ((int) ((1.0f / deltaR) * 1000));
		}
		default:
			break;
		}
		return 0.0f;
	}

	/**
	 * Function triggers reading of UDP socket for currently available data for
	 * specified side
	 * 
	 * @param cameraSide
	 * @return
	 */
	public Vec2f[] getDataset(int cameraSide) {
		switch (cameraSide) {
		// left camera
		case 0:
			lockLeft.readLock().lock();
			String msgL = udpMessageLeft;
			lockLeft.readLock().unlock();
			String[] partsL = msgL.split(";");
			//Extract hand angle
			handAngleLeft=Float.parseFloat((partsL[partsL.length-2]));
			//extract other values
			processMessage(partsL, coordinateSetsLeft,handAngleLeft);
			return coordinateSetsLeft;
		// right camera
		case 1:
			lockRight.readLock().lock();
			String msgR = udpMessageRight;
			lockRight.readLock().unlock();
			String[] partsR = msgR.split(";");
			//Extract hand angle
			handAngleRight=Float.parseFloat((partsR[partsR.length-2]));
			//extract other values
			processMessage(partsR, coordinateSetsRight,handAngleRight);
			return coordinateSetsRight;
		default:
			System.out.println("invalid camera input parameter " + cameraSide);
			return new Vec2f[0];
		}

	}

	/*
	 * Raspberry Pi 2 camera Params camera Dist= 50–75 mm for human vision viewing
	 * angle in degree v2 module x:62.2 y:48.8
	 */
	/**
	 * 
	 * @param rightX
	 * @param leftX
	 * @return
	 */
	// TODO use vector distance?
	public float calculateZDistance(float rightX, float leftX)

	{
		double calculatedZDistance = 0.0f;
		// compensated Calculation formula Z=k*d^z
		double k = 4543.320129238;
		double z = -1.0354229928;
		double disparity = rightX - leftX;
		try {
			disparity = oeurFilterZ.filter(disparity);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		calculatedZDistance = k * (1.0 / (Math.pow(Math.abs(disparity), -z)));

		// Clamp depth ranges to known values of the tracking space
		calculatedZDistance = clamp(0.0, 90.0, calculatedZDistance);
		// Coarse value Filtering
		if (!Double.isNaN(calculatedZDistance)) {
			return (float) (-calculatedZDistance + zeroPlaneOffset) * 10.0f;
		}
		return 0.0f;
	}

	private double clamp(double min, double max, double value) {
		return Math.max(min, Math.min(value, max));

	}

	public int getUDPPortLeft() {
		return udpPortLeft;
	}

	public void setUDPPortLeft(int uDPPortLeft) {
		udpPortLeft = uDPPortLeft;
	}

	public int getUDPPortRight() {
		return udpPortRight;
	}

	public void setUDPPortRight(int uDPPortRight) {
		udpPortRight = uDPPortRight;
	}

	public int[] getResZ() {
		return resZ;
	}

	public Vec2f[] getCoordinateSetsLeft() {
		return coordinateSetsLeft;

	}

	public Vec2f[] getCoordinateSetsRight() {
		return coordinateSetsRight;
	}

	public long getEpochRight() {
		return epochRight;
	}

	public long getEpochLeft() {
		return epochLeft;
	}

	public float getHandAngleLeft() {
		return handAngleLeft;
	}

	public float getHandAngleRight() {
		return handAngleRight;
	}

}
