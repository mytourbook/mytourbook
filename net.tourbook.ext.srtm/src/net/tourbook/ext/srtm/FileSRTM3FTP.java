package net.tourbook.ext.srtm;

import java.io.*;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPMessageCollector;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FTPConnectMode;

public final class FileSRTM3FTP {

   final static String host = "e0srp01u.ecs.nasa.gov";
   final static String user = "anonymous";
   final static String password = "";

   final static int DirEurasia = 0;
   final static int DirNorth_America = 1;
   final static int DirSouth_America = 2;
   final static int DirAfrica = 3;
   final static int DirAustralia = 4;
   final static int DirIslands = 5;
   
   static String[] dirs = {"/srtm/version2/SRTM3/Eurasia",              // 0
                           "/srtm/version2/SRTM3/North_America",        // 1
                           "/srtm/version2/SRTM3/South_America",        // 2
                           "/srtm/version2/SRTM3/Africa",               // 3
                           "/srtm/version2/SRTM3/Australia",            // 4
                           "/srtm/version2/SRTM3/Islands"};             // 5
   
   public final static void get(String remoteName, String localName) {
    
      try {
         String remoteDirName = getDir(remoteName);
         String localDirName = localName.substring(0, localName.lastIndexOf(File.separator));         
         System.out.println("remoteDir " + remoteDirName);
         System.out.println("localDir " + localDirName);
         
         File localDir = new File(localDirName);
         if (!localDir.exists()) {
            System.out.println("create Dir " + localDirName);
            localDir.mkdir();
         }
         
         final FTPClient ftp = new FTPClient();
         final FTPMessageCollector listener = new FTPMessageCollector();
         
         ftp.setRemoteHost(host);
         ftp.setMessageListener(listener);

         System.out.println("connect " + host);
         ftp.connect();

         System.out.println("login " + user + " " + password);
         ftp.login(user, password);

         System.out.println("set passive mode");
         ftp.setConnectMode(FTPConnectMode.PASV);

         System.out.println("set type binary");
         ftp.setType(FTPTransferType.BINARY);

         System.out.println("chdir " + remoteDirName);
         ftp.chdir(remoteDirName);

         System.out.println("get " + remoteName + " -> " + localName + " ...");
         ftp.get(localName, remoteName);

         System.out.println("quit");
         ftp.quit();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }   

   private static String getDir(String pathName) throws Exception {

      String fileName = pathName.substring(pathName.lastIndexOf(File.separator)+1);
      String latString = fileName.substring(0, 3)+":00"; // z.B. N50
      String lonString = fileName.substring(3, 7)+":00"; // z.B. E006
      GeoLat lat = new GeoLat(latString);
      GeoLon lon = new GeoLon(lonString);
      
      if (lat.groesser(new GeoLat("N60:00"))) throw(new FileNotFoundException());
      if (lat.kleiner (new GeoLat("S56:00"))) throw(new FileNotFoundException());
      
      // Reihenfolge wichtig!
      // vgl. Grafik ftp://e0srp01u.ecs.nasa.gov/srtm/version2/Documentation/Continent_def.gif
      if (isIn(lat, lon, "S56", "S28", "E165", "E179")) return dirs[DirIslands];
      if (isIn(lat, lon, "S56", "S55", "E158", "E159")) return dirs[DirIslands];
      if (isIn(lat, lon, "N15", "N30", "W180", "W155")) return dirs[DirIslands];
      if (isIn(lat, lon, "S44", "S05", "W030", "W006")) return dirs[DirIslands];
      if (isIn(lat, lon, "S56", "S45", "W039", "E060")) return dirs[DirIslands];
      if (isIn(lat, lon, "N35", "N39", "W040", "W020")) return dirs[DirAfrica];
      if (isIn(lat, lon, "S20", "S20", "E063", "E063")) return dirs[DirAfrica];
      if (isIn(lat, lon, "N10", "N10", "W110", "W110")) return dirs[DirNorth_America];
      if (isIn(lat, lon, "S10", "N14", "W180", "W139")) return dirs[DirEurasia];
      if (isIn(lat, lon, "S13", "S11", "E096", "E105")) return dirs[DirEurasia];
      if (isIn(lat, lon, "S44", "S11", "E112", "E179")) return dirs[DirAustralia];
      if (isIn(lat, lon, "S28", "S11", "W180", "W106")) return dirs[DirAustralia];
      if (isIn(lat, lon, "S35", "N34", "W030", "E059")) return dirs[DirAfrica];
      if (isIn(lat, lon, "N35", "N60", "W011", "E059")) return dirs[DirEurasia];
      if (isIn(lat, lon, "S10", "N60", "E060", "E179")) return dirs[DirEurasia];
      if (isIn(lat, lon, "N15", "N60", "W180", "W043")) return dirs[DirNorth_America];
      if (isIn(lat, lon, "S56", "N14", "W093", "W033")) return dirs[DirSouth_America];
      return dirs[DirIslands];
   }
   
   private static boolean isIn(GeoLat lat, GeoLon lon, 
         String latMin, String latMax, String lonMin, String lonMax) {

      if (lat.kleiner (new GeoLat(latMin+":00"))) return false;
      if (lat.groesser(new GeoLat(latMax+":00"))) return false;
      if (lon.kleiner (new GeoLon(lonMin+":00"))) return false;
      if (lon.groesser(new GeoLon(lonMax+":00"))) return false;
      return true;
   }
   
   public static void main(String[] args) {

   }
} 