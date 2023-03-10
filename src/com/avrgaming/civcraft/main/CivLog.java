/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.bukkit.plugin.java.JavaPlugin;

import com.avrgaming.civcraft.exception.CivException;

public class CivLog {

	public static JavaPlugin plugin;
	private static Logger cleanupLogger;
	
	public static void init(JavaPlugin plugin) {
		CivLog.plugin = plugin;
		
		cleanupLogger = Logger.getLogger("cleanUp");
		FileHandler fh;
		
		try {
			fh = new FileHandler("cleanUp.log");
			cleanupLogger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void heading(String title) {
		plugin.getLogger().info("========= "+title+" =========");
	}
	
	public static void info(String message) {
		plugin.getLogger().info(message);
	}
	
	public static void debug(String message) {
		plugin.getLogger().info("[DEBUG] "+message);
	}

	public static void warning(String message) {
		if (message == null) {
			try {
				throw new CivException("Null warning message!");
			} catch (CivException e){
				e.printStackTrace();
			}
		}
		if (CivGlobal.warningsEnabled) {
			plugin.getLogger().info("[WARNING] "+message);
		}
	}

	public static void error(String message) {
		plugin.getLogger().severe(message);
	}
	
	public static void adminlog(String name, String message) {
		plugin.getLogger().info("[ADMIN:"+name+"] "+message);
	}
	
	public static void cleanupLog(String message) {
		info(message);
		cleanupLogger.info(message);		
	}
	
	public static void exception(String string, Exception e) {
		//TODO log the exception in civexceptions file.
		e.printStackTrace();		
	}

	public static void moneylog(final String name, final String message) {
        CivLog.plugin.getLogger().info("[\u0414\u0435\u0431\u0430\u0433 \u0434\u0435\u043d\u044c\u0433\u043e\u0431\u043e\u0440\u043e\u0442\u0430: " + name + "] " + message);
    }

	public static String paste(final String contents, final String extension, final String customName, final String deleteKey) {
        HttpURLConnection pasteConnection = null;
        try {
            pasteConnection = (HttpURLConnection)new URL("https://drop.xtrafrancyz.net/upload/" + customName + "." + extension).openConnection();
            pasteConnection.setConnectTimeout(3000);
            pasteConnection.setReadTimeout(3000);
            pasteConnection.setRequestProperty("Linx-Expiry", "2592111");
            pasteConnection.setRequestProperty("Linx-Randomize", "yes");
            pasteConnection.setRequestProperty("Linx-Delete-Key", deleteKey);
            pasteConnection.setRequestMethod("PUT");
            pasteConnection.setDoOutput(true);
            final OutputStream out = pasteConnection.getOutputStream();
            out.write(contents.getBytes("UTF-8"));
            out.flush();
            return new BufferedReader(new InputStreamReader(pasteConnection.getInputStream())).readLine();
        }
        catch (Exception e) {
            return e.getMessage();
        }
        finally {
            if (pasteConnection != null) {
                pasteConnection.disconnect();
            }
        }
    }
}
