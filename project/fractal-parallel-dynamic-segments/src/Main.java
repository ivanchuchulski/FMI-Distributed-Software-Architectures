import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import javax.imageio.ImageIO;

import org.apache.commons.cli.*;

public class Main {
	public static int width = 640;
	public static int height = 480;

	public static int threadCount = 1;

	public static double minReal = -2.0;
	public static double maxReal = 2.0;
	public static double minImaginary = -2.0;
	public static double maxImaginary = 2.0;

	public static String outputFileName = "zad17.png";

	public static boolean quietOutput = false;

	public static int maxPointIterations = 500;

	public static int granularity = 1;

	public static void main(String[] args) {
		long startTimestamp = Calendar.getInstance().getTimeInMillis();

		try {
			parseConsoleInput(args);
		}
		catch (ParseException parseException) {
			parseException.printStackTrace();
			System.exit(1);
		}

		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

		Graphics2D graphics2D = bufferedImage.createGraphics();
		graphics2D.setColor(Color.WHITE);
		graphics2D.fillRect(0, 0, width - 1, height - 1);

		int heightStep = (int) Math.ceil((float)height / (threadCount * granularity));
		int widthStep = (int)Math.ceil((float)width / (threadCount * granularity));

		int segmentIDCounter = 0;
		int threadIDCounter = 0;
		int numberOfSegments = threadCount * granularity * threadCount * granularity;
		ArrayBlockingQueue<SegmentTask> tasks = new ArrayBlockingQueue<>(numberOfSegments, true);

		for (int heightCounter = 0; heightCounter < height; heightCounter += heightStep) {
			for (int widthCounter = 0; widthCounter < width; widthCounter += widthStep) {
				int heightEnd = heightCounter + heightStep;
				int widthEnd = widthCounter +  widthStep;

				if (heightEnd > height) {
					heightEnd = height;
				}

				if (widthEnd > width) {
					widthEnd = width;
				}

				SegmentTask segmentTask = new SegmentTask(segmentIDCounter, heightCounter, widthCounter, heightEnd, widthEnd);
				tasks.add(segmentTask);
				segmentIDCounter++;
			}
		}

//		for (SegmentTask task : tasks) {
//			System.out.printf("%d %d %d %d%n", task.heightStart, task.heightEnd, task.widthStart, task.widthEnd);
//		}
//		System.exit(2);

		ArrayList<FractalPartThread> fractalThreads = new ArrayList<>(threadCount);
		for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
			FractalPartThread fractalPartThread = new FractalPartThread(threadIndex, bufferedImage, tasks);

			fractalThreads.add(fractalPartThread);

			fractalPartThread.start();
		}

		// wait for all threads to finish their jobs
		try {
			for (FractalPartThread fractalPartThread : fractalThreads) {
				fractalPartThread.join();
			}
		}
		catch (InterruptedException interruptedException) {
			interruptedException.printStackTrace();
		}

		graphics2D.setColor(Color.GRAY);
		graphics2D.drawRect(0, 0, width - 2, height - 2);

		try {
			ImageIO.write(bufferedImage, "PNG", new File(outputFileName));
		}
		catch (IOException ioException) {
			ioException.printStackTrace();
		}

//		or maybe stop timer before image write?
		long finishTimestamp = Calendar.getInstance().getTimeInMillis();
		long overallTime = finishTimestamp - startTimestamp;

		System.out.printf("Total execution time for current run (millis): %d%n", overallTime);

		if (!quietOutput) {
			System.out.printf("Threads used in current run: %d%n", threadCount);
		}
	}

	private static void parseConsoleInput(String[] args) throws ParseException {
		Options options = new Options();

		options.addOption("s", true, "output image dimensions e.g. 640x480");
		options.addOption("r", true, "complex plane bounds e.g. -2.0:2.0:-1.0:1.0");
		options.addOption("t", true, "number of tasks(threads)");
		options.addOption("o", true, "name of the output file");
		options.addOption("q", false, "quiet running of program, without additional logs");
		options.addOption("h", false, "print information about the program");

		// for testing
		options.addOption("i", true, "number of iterations");
		options.addOption("g", true, "granularity");

		CommandLineParser parser = new DefaultParser();
		CommandLine commandLine = parser.parse(options, args);

		if (commandLine.hasOption("h")) {
			printHelp(options);
			System.exit(1);
		}

		if (commandLine.hasOption("s")) {
			String[] imageDimensions = commandLine.getOptionValue("s").split("x");
			width = Integer.parseInt(imageDimensions[0]);
			height = Integer.parseInt(imageDimensions[1]);
		}

		if (commandLine.hasOption("r")) {
			String[] complexPlaneBounds = commandLine.getOptionValue("r").split(":");
		 	minReal = Double.parseDouble(complexPlaneBounds[0]);
		 	maxReal = Double.parseDouble(complexPlaneBounds[1]);
		 	minImaginary = Double.parseDouble(complexPlaneBounds[2]);
		 	maxImaginary = Double.parseDouble(complexPlaneBounds[3]);
		}

		if (commandLine.hasOption("t")) {
			threadCount = Integer.parseInt(commandLine.getOptionValue("t"));
		}

		if (commandLine.hasOption("o")) {
			outputFileName = commandLine.getOptionValue("o");
		}

		if (commandLine.hasOption("q")) {
			quietOutput = true;
		}

		if (commandLine.hasOption("i")) {
			maxPointIterations = Integer.parseInt(commandLine.getOptionValue("i"));
		}

		if (commandLine.hasOption("g")) {
			granularity = Integer.parseInt(commandLine.getOptionValue("g"));
		}
	}

	private static void printHelp(Options options) {
		String header = "program calculating fractal image\n";
		String footer = "author : Ivan Chuchulski\n";
		HelpFormatter helpFormatter = new HelpFormatter();

		helpFormatter.printHelp("runMe", header, options, footer);
	}
}
