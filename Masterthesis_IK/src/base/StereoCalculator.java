package base;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import au.edu.federation.utils.Vec2f;

public class StereoCalculator {
	public int[] resZ;
	private int UDPPortLeft;
	private int UDPPortRight;
	public Vec2f[] coordinateSetsLeft;
	public Vec2f[] coordinateSetsRight;
	private static final ReentrantReadWriteLock lockLeft = new ReentrantReadWriteLock(true);
	public volatile static String UDPMessageLeft, UDPMessageRight;
	private static final ReentrantReadWriteLock lockRight = new ReentrantReadWriteLock(true);
	private final int cameraImageWidth = 640;
	private final int cameraIamgeHeight = 480;
	private int zeroPlaneOffset = 90;
	private OneEuroFilter oeurFilterX;
	private OneEuroFilter oeurFilterY;
	private OneEuroFilter oeurFilterZ;
    private double frequency = 30; // Hz
    private double mincutoffZ = 2.0; 
    private double betaZ =0.001;     
     
    private double mincutoffY= 0.75; // FIXME
    private double betaY = 5.0;      // FIXME
    
    private double mincutoffX = 1.0; // FIXME
    private double betaX = 2.0;      // FIXME



	public StereoCalculator(int numElements) {
		this.resZ = new int[numElements];
		this.UDPPortLeft = 8888;
		this.UDPPortRight = 9999;
		this.coordinateSetsLeft = new Vec2f[numElements];
		this.coordinateSetsRight = new Vec2f[numElements];
		for (int i = 0; i < coordinateSetsLeft.length; i++) {
			coordinateSetsLeft[i] = new Vec2f(0, 0);
			coordinateSetsRight[i] = new Vec2f(0, 0);
			resZ[i] = 0;
		}
		try {
			oeurFilterZ= new OneEuroFilter(frequency, mincutoffZ, betaZ);
			oeurFilterY= new OneEuroFilter(frequency, mincutoffY, betaY);
			oeurFilterX= new OneEuroFilter(frequency, mincutoffX, betaX);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class UDPProcessThreadLeft implements Runnable {
		private int UDPListenPort;
		private boolean running = true;
		private DatagramSocket dsocket;
		private byte[] buffer;
		private DatagramPacket packet;
		private ReentrantReadWriteLock lock;

		public UDPProcessThreadLeft(int UDPPort, ReentrantReadWriteLock lock) throws SocketException {
			this.UDPListenPort = UDPPort;
			this.dsocket = new DatagramSocket(this.UDPListenPort);
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
						StereoCalculator.UDPMessageLeft = msg;
					} finally {
						lock.writeLock().unlock();
					}
				}
				// Reset the length of the packet before reusing it.
				packet.setLength(buffer.length);
			} catch (

			SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
		private int UDPListenPort;
		private boolean running = true;
		private DatagramSocket dsocket;
		private byte[] buffer;
		private DatagramPacket packet;
		private ReentrantReadWriteLock lock;

		public UDPProcessThreadRight(int UDPPort, ReentrantReadWriteLock lock) throws SocketException {
			this.UDPListenPort = UDPPort;
			this.dsocket = new DatagramSocket(this.UDPListenPort);
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
						StereoCalculator.UDPMessageRight = msg;
					} finally {
						lock.writeLock().unlock();
					}
				}
				// Reset the length of the packet before reusing it.
				packet.setLength(buffer.length);
			} catch (

			SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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

	// public static void main(String[] args) throws IOException {
	// StereoCalculator calc = new StereoCalculator(5);
	// calc.createUDPListener(8888, 0);
	// while (true) {
	// calc.getDataset(0);
	//
	// }
	//
	// }

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
			// dSocketL = new DatagramSocket(port);
			Thread udpthreadLeft = new Thread(new UDPProcessThreadLeft(port, lockLeft));
			udpthreadLeft.start();
			break;
		case 1:
			// dSocketR = new DatagramSocket(port);
			Thread udpthreadRight = new Thread(new UDPProcessThreadRight(port, lockRight));
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
	 *            The split parts of the received UDP String message containig the
	 *            tracking data
	 * @param coordinateSet
	 *            The 2D vector into which the extracted data will be written
	 */
	private void processMessage(String[] parts, Vec2f[] coordinateSet) {
		double t1 = System.nanoTime();
		for (int i = 0; i < parts.length; i++) {
			String[] values = parts[i].split(",");
			// Extract value by extractng substrngs at correct positions
			try {
				int xVal = Integer.parseInt(values[0].substring(1));
				int yVal = Integer.parseInt(values[1].substring(1, values[1].length() - 1));
				// TODO variable for clamping
				xVal = (int) clamp(0.0, 640.0, (double) (xVal));
				yVal =(int) clamp(0.0, 480.0, (double) (yVal));
				//filter Values
				
				//yVal=(float) oeurFilterY.filter(yVal);
				// TODO handle tracking loss with synchronization
				//
				/* if (xVal > 0.0f && yVal > 0.0f) { */
				
				coordinateSet[i].x = (float) oeurFilterX.filter( -(xVal - (cameraImageWidth / 2.0)));
				coordinateSet[i].y = (float) oeurFilterY.filter((yVal - (cameraIamgeHeight / 2.0)));
				// }
			} catch (Exception e) {
				System.out.println(e);
			}
		}
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
			String msgL = UDPMessageLeft;
			lockLeft.readLock().unlock();
			String[] partsL = msgL.split(";");
			processMessage(partsL, coordinateSetsLeft);
			return coordinateSetsLeft;
		// right camera
		case 1:
			lockRight.readLock().lock();
			String msgR = UDPMessageRight;
			lockRight.readLock().unlock();

			String[] partsR = msgR.split(";");
			processMessage(partsR, coordinateSetsRight);
			return coordinateSetsRight;
		default:
			System.out.println("invalid camera input parameter " + cameraSide);
			return null;
		}

	}

	/*
	 * Raspberry Pi 2 camera Params camera Dist= 50–75 mm for human vision viewing
	 * angle in degree v2 module x:62.2 y:48.8
	 */
	/**
	 * Function calculates Z-Distance from the trviaacking values of the two cameras
	 * 
	 * @param stereoBaseDistance
	 *            in m
	 * @param cameraAngle
	 *            in deg
	 * @param numHorPix
	 * @return
	 */
	public float calculateZDistance(float stereoBaseDistance, float cameraAngle, int numHorPix, float rightX,
			float leftX, float currentZ)

	{
		double calculatedZDistance = 0.0f;
		double deltaZ = 0.0f;
		if (stereoBaseDistance == 0.0f) {
			// distance default value for camera is 75mm which
			// corresponds to a value from the range of the human interpupilary disitance
			stereoBaseDistance = 0.075f;
		}
		if (cameraAngle == 0.0f) {
			cameraAngle = 62.2f;
		}

		float alignmentCompensation = 0.0f;
		// compensated Calculation formula Z=k*d^z
		double k = 4543.320129238;
		double z = -1.0354229928;
		double disparity = rightX - leftX;
		calculatedZDistance = k * (1.0 / (Math.pow(Math.abs(disparity), -z)));
		// TODO Variable for min and max
		try {
			double filteredValue= oeurFilterZ.filter(calculatedZDistance);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Clamp depth ranges to known values of the tracking space
		calculatedZDistance = clamp(0.0, 90.0, calculatedZDistance);
		// TODO clamp distance delta to reasonable value
		// deltaZ = Math.abs(currentZ - calculatedZDistance);
		// TODO Catch NaN
		return (float) -calculatedZDistance + zeroPlaneOffset;

	}

	private double clamp(double min, double max, double value) {
		return Math.max(min, Math.min(value, max));

	}

	public int getUDPPortLeft() {
		return UDPPortLeft;
	}

	public void setUDPPortLeft(int uDPPortLeft) {
		UDPPortLeft = uDPPortLeft;
	}

	public int getUDPPortRight() {
		return UDPPortRight;
	}

	public void setUDPPortRight(int uDPPortRight) {
		UDPPortRight = uDPPortRight;
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

}
