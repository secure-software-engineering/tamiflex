package de.bodden.tamiflex.playout;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

public class ScreenshotCreator implements Runnable {
	
		public void run() {
			
			while(true) {
				
				//wait for 1 sec
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				//produce screenshot
				//store file name of screenshot in a global variable
				//when next call is recorded then add comment with name of file
				//if there are two screenshots A and B without any refl call in between then delete A again
			
				try {
				/*	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
					GraphicsDevice[] gs = ge.getScreenDevices();
					BufferedImage[] screenshots = new BufferedImage[gs.length];
					 
					DisplayMode mode;
					Rectangle bounds;
					 
					for(int i=0; i<gs.length; i++)
					{
					    mode = gs[i].getDisplayMode();
					    bounds = new Rectangle(0, 0, mode.getWidth(), mode.getHeight());
					    screenshots[i] = new Robot(gs[i]).createScreenCapture(bounds);
					    screenshots[i].getData(); // .getSource();
					    System.out.println("done");
					}*/
					
					
					    Rectangle captureSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
				        BufferedImage bufferedImage = new Robot().createScreenCapture(captureSize);
				        ImageIO.write(bufferedImage, "bmp", new File("outt"));
				        /*   // RenderedImage rendImage = bufferedImage;
				        // ImageIO.write(bufferedImage,"jpg",new File("out/"));
				        // ImageIO.write(bufferedImage, "png", File ("out"));
				        // ImageIO.getImageReaders(bufferedImage);
				        // write(bufferedImage, "png", new File(String fileName));
				        System.out.println("done");*/
				      
					 	
				        
				}
				    catch(AWTException e) {
				    	System.err.println("Someone call a doctor!");
				   } 
				    /*catch (IOException e) {
				    	System.err.println("je ss là");
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/ catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					System.out.println("create screenshot");
			}					
		}

		private ImageOutputStream File(String string) {
			// TODO Auto-generated method stub
			return null;
		};

}
