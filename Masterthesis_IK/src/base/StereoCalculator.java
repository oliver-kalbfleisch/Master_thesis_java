package base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import au.edu.federation.utils.Vec2f;

public class StereoCalculator {
	public int[] resZ;
	private int udpPortLeft;
	private int udpPortRight;
	private Vec2f[] coordinateSetsLeft;
	private Vec2f[] coordinateSetsRight;
	private static final ReentrantReadWriteLock lockLeft = new ReentrantReadWriteLock(true);
	public volatile static String udpMessageLeft, udpMessageRight;
	private static final ReentrantReadWriteLock lockRight = new ReentrantReadWriteLock(true);
	private static final int CAMERA_IMAGE_WIDTH = 640;
	private static final int CAMERA_IMAGE_HEIGHT = 480;
	private long epochRight = 0;
	private long epochLeft = 0;
	private int delta =80s;
	private static long threadTimingDelta = 0;
	private int zeroPlaneOffset = 90;
	private OneEuroFilter oeurFilterX;
	private OneEuroFilter oeurFilterY;
	private OneEuroFilter oeurFilterZ;
	private static float xfreq, yfreq, zfreq;
	Thread udpthreadLeft;
	Thread udpthreadRight;

	private double initialFrequency = 30; // Hz
	private double mincutoffZ = 1.0;
	private double betaZ = 0.0;

	private double mincutoffY = 1.0;
	private double betaY = 0.0007;

	private double mincutoffX = 1.0;
	private double betaX = 0.0007;

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
			oeurFilterZ = new OneEuroFilter(initialFrequency, mincutoffZ, betaZ);
			oeurFilterY = new OneEuroFilter(initialFrequency, mincutoffY, betaY);
			oeurFilterX = new OneEuroFilter(initialFrequency, mincutoffX, betaX);
		} catch (Exception e) {
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
			this.buffer = new byte[1024];
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
						epochLeft = Long.parseLong(msg.substring(msg.length() - 14, msg.length() - 1));

						if (epochLeft - epochRight < delta) {
							udpMessageLeft = msg;
						}

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
			this.buffer = new byte[1024];
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
						epochRight = Long.parseLong(msg.substring(msg.length() - 14, msg.length() - 1));
						if (epochLeft - epochRight > -delta) {
							// System.out.println("rt:"+(epochLeft-epochRight));
							udpMessageRight = msg;
						}
						;

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
	private void processMessage(String[] parts, Vec2f[] coordinateSet, long epoch) {
		for (int i = 0; i < parts.length - 1; i++) {
			String[] values = parts[i].split(",");
			// Extract value by extracting substrings at correct positions
			try {
				int xVal = Integer.parseInt(values[0].substring(1));
				int yVal = Integer.parseInt(values[1].substring(1, values[1].length() - 1));
				xVal = (int) clamp(0.0, 640.0, (double) (xVal));
				yVal = (int) clamp(0.0, 480.0, (double) (yVal));
				// filter Values
				float currentX = (float) oeurFilterX.filter(-(xVal - (CAMERA_IMAGE_WIDTH / 2.0)));
				float currentY = (float) oeurFilterY.filter((yVal - (CAMERA_IMAGE_HEIGHT / 2.0)));
				//float currentX = (float) -(xVal - (CAMERA_IMAGE_WIDTH / 2.0));
				//float currentY = (float) (yVal - (CAMERA_IMAGE_HEIGHT / 2.0));
				// Filter out jumps in tracking data
				coordinateSet[i].x = currentX;
				coordinateSet[i].y = currentY;
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		// System.out.println("TimeDelta" + side + ": " + delta);
	}
	
	private int calculateCameraUpdateFrequency(int side)
	{
		
	}

	/**
	 * Function triggers reading of UDP socket for currently available data for
	 * specified side
	 * 
	 * @param cameraSide
	 * @return
	 */
	public Vec2f[] getDataset(int cameraSide) {
		double t1 = System.nanoTime();
		switch (cameraSide) {
		// left camera
		case 0:
			lockLeft.readLock().lock();
			String msgL = udpMessageLeft;
			lockLeft.readLock().unlock();
			String[] partsL = msgL.split(";");
			processMessage(partsL, coordinateSetsLeft, epochLeft);
			return coordinateSetsLeft;
		// right camera
		case 1:
			lockRight.readLock().lock();
			String msgR = udpMessageRight;
			lockRight.readLock().unlock();
			String[] partsR = msgR.split(";");
			processMessage(partsR, coordinateSetsRight, epochRight);
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
		calculatedZDistance = k * (1.0 / (Math.pow(Math.abs(disparity), -z)));
		try {
			calculatedZDistance = oeurFilterZ.filter(calculatedZDistance);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Clamp depth ranges to known values of the tracking space
		calculatedZDistance = clamp(0.0, 90.0, calculatedZDistance);
		// Coarse value Filtering
		return (float) -calculatedZDistance + zeroPlaneOffset;
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

}
