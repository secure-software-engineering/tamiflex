package de.bodden.tamiflex.playout;
import java.awt.image.RenderedImage;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.imageio.ImageIO;
import java.lang.Runnable;

public class ScreenCapture implements Runnable {
	
	public void run() {
		while(true) {
			
			//wait for 1 sec
			try {
				Thread.sleep(1000);
			}catch (InterruptedException e) {
			}
			
			//try {
				new ScreenCapture().captureFullScreen();
				
			/*} catch (InterruptedException e) {
			}*/
		}
	
	}

	/**
	 * A Java program to capture the full screen to an image file.
	 * Uses a Properties file located in the same directory as this compiled
	 * class, and should be named ScreenCapturer.dat. If not, it is created.
	 * When started, the program creates an image file in a directory specified by
	 * the SAVE_LOCATION property and the format is specified in the IMAGE_TYPE
	 * property.
	 *
	 * @author Dick Larsson
	 * @author Johan Känngård, http://dev.kanngard.net/
	 */
	
	    /**
	     * Properties that contains the keys IMAGE_NAME, IMAGE_TYPE and SAVE_LOCATION.
	     */
	    Properties properties = new Properties();

	    /**
	     * The name of the properties file to load.
	     */
		String propertiesFileName = "ScreenCapturer.dat";

	    /**
	     * The target location path of the screen capture image file.
	     */
		String saveLocation = "/home/hela/shot";

	    /**
	     * The image type to create. Can be png, gif, jpg etc.
	     */
		String imageType = null;

	    /**
	     * The name of the image to create. This program adds a timestamp
	     * to the name also.
	     */
		String imageName = null;

	    /**
	     * Main entry for this Java program.
	     *
	     * @param args the command line arguments (not used).
	     */
		
		/**
		 * store file name of screenshot in a global variable
		 */
		String globalImageName = null;
		
	 /*   public static void main(String args[]) {
	        new ScreenCapture().captureFullScreen();
	    } */

	    /**
	     * Default constructor.
	     */
	   public ScreenCapture() {
	        loadProperties(propertiesFileName);
		}


	    /**
	     * Loads the properties file specified in propertiesFileName.
	     * If the file in propertiesFileName doesn't exist, it is created
	     * with the following default values:
	     * IMAGE_NAME=SCREENSHOT
	     * IMAGE_TYPE=png
	     * SAVE_LOCATION=c:\\temp => changed to /home/hela 
	     *
	     * @param propertiesFileName the Properties file to load.
	     */
		private void loadProperties(String propertiesFileName) {
	        try {
				FileInputStream in = new FileInputStream(propertiesFileName);
				properties.load(in);
				in.close();
				
			} catch(FileNotFoundException e) {
				properties.setProperty("IMAGE_NAME", "SCREENSHOT");
				properties.setProperty("IMAGE_TYPE", "png");
				//properties.setProperty("SAVE_LOCATION", "home");
				setProperties(properties);
			
			} catch(Exception e) {
	            e.printStackTrace();
			}
			imageName = properties.getProperty("IMAGE_NAME");
			imageType = properties.getProperty("IMAGE_TYPE");
			//System.out.println("heeeeeeeeeeeeeeela");
			//saveLocation = properties.getProperty("SAVE_LOCATION");
		}

	    /**
	     * Writes the specified Properties to the file specified in propertiesFileName.
	     *
	     * @param p the Properties to write to disk.
	     */
	    private void setProperties(Properties p) {
	        try {
	            p.store(new FileOutputStream(propertiesFileName), null);
	        } catch(IOException e) {
	            System.err.println("Exception while saving properties file. " + e);
	        }
	    }

	    /**
	     * Captures the entire screen and writes it to the location specified by
	     * the SAVE_LOCATION property in the propertiesFileName file. The format is
	     * specified with the IMAGE_TYPE variable. The name of the file is set to a
	     * combination of IMAGE_NAME, System.currentTimeMillis() and the IMAGE_TYPE.
	     */
	    public void captureFullScreen() {
	        try {
				Robot robot = new Robot();
	            Rectangle area = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
	            File target = new File(saveLocation, imageName + "_" + System.currentTimeMillis() + "." + imageType);
	            System.out.println("heeeeeeeeeeeeeeela");
	            //System.out.println(imageName + "_" + System.currentTimeMillis() + "." + imageType);
	            globalImageName = imageName + "_" + System.currentTimeMillis() + "." + imageType;
	            System.out.println(globalImageName);
	            saveImageToFile(robot.createScreenCapture(area), target);
	        } catch (AWTException e) {
	            System.err.println("Exception while capturing screen. " + e);
	        }
	    }

	    /**
	     * Saves the specified RenderedImage to the specified File.
	     *
	     * @param renderedImage the image to write to file.
	     * @param target the file to write the image to.
	     */
	    private void saveImageToFile(RenderedImage renderedImage, File target) {
	        try {
	            ImageIO.write(renderedImage, imageType, target);
	        } catch (IOException e) {
	            System.err.println(e);
	        }
	    }
	
}
