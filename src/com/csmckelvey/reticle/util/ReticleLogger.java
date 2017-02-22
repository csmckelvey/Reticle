package com.csmckelvey.reticle.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReticleLogger {

	private static boolean debug = false;
	private static boolean outputToFile = false;
	private static String outputFileName = null;
	private static String exceptionOutputFileName = null;

	private static ReticleLogger instance = new ReticleLogger();
	
	private ReticleLogger() {}
	
	public static ReticleLogger getLogger() {
		if (instance == null) {
			instance = new ReticleLogger();
		}
		return instance;
	}
	
	public ReticleLogger log(String message) {
		instance.log(message, 0, 0);
		return instance;
	}
		
	public ReticleLogger log(String message, int tabs, int extraNewLines) {
		log(message, tabs, extraNewLines, debug);
		return instance;
	}

	public ReticleLogger log(String message, int tabs, int extraNewlines, boolean override) {
		if (debug || override) {
			StringBuilder output = new StringBuilder();
			
			output.append(LocalDate.now()).append(" ").append(LocalTime.now()).append(" ");
			
			for (int i = 0; i < tabs; i++) { 
				output.append("\t"); 
			}
			
			output.append(message);
			
			for (int i = 0; i < extraNewlines; i++) { 
				output.append("\n"); 
			}
			
			System.out.println(output.toString());
			
			if (outputToFile && outputFileName != null) {
				try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFileName, true)))) {
					out.println(output.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return instance;
	}
	
	public ReticleLogger logException(Exception exception) {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(exceptionOutputFileName, true)))) {
			exception.printStackTrace(out);
			exception.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instance;
	}
	
	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(boolean debug) {
		ReticleLogger.debug = debug;
	}

	public static boolean isOutputToFile() {
		return outputToFile;
	}

	public static void setOutputToFile(boolean outputToFile) {
		ReticleLogger.outputToFile = outputToFile;
	}
	
	public static String getOutputFileName() {
		return outputFileName;
	}

	public static void setOutputFileName(String outputFileName) {
		ReticleLogger.outputFileName = outputFileName;
	}
	
	public static String getExceptionOutputFileName() {
		return exceptionOutputFileName;
	}

	public static void setExceptionOutputFileName(String exceptionOutputFileName) {
		ReticleLogger.exceptionOutputFileName = exceptionOutputFileName;
	}
	
}
