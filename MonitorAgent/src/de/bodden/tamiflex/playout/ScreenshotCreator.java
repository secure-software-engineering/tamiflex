package de.bodden.tamiflex.playout;

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
				System.out.println("create screenshot");
			}					
		};

}
