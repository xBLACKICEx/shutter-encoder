/*******************************************************************************************
* Copyright (C) 2021 PACIFICO PAUL
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
* 
********************************************************************************************/

package functions.other;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import application.Ftp;
import application.Settings;
import application.Shutter;
import application.Utils;
import application.VideoPlayer;
import application.Wetransfer;
import library.FFMPEG;
import library.FFPROBE;

public class Rewrap extends Shutter {
	
	
	private static int complete;
	
	public static void main() {
		
		Thread thread = new Thread(new Runnable(){			
			@Override
			public void run() {
				if (scanIsRunning == false)
					complete = 0;
				
				lblTermine.setText(Utils.completedFiles(complete));

				for (int i = 0 ; i < liste.getSize() ; i++)
				{
					File file = new File(liste.getElementAt(i));
					
					//SCANNING
		            if (Shutter.scanIsRunning)
		            {
		            	file = Utils.scanFolder(liste.getElementAt(i));
		            	if (file != null)
		            		btnStart.setEnabled(true);
		            	else
		            		break;
		            	Shutter.progressBar1.setIndeterminate(false);		
		            }
		            else if (Settings.btnWaitFileComplete.isSelected())
		            {
						if (Utils.waitFileCompleted(file) == false)
							break;
		            }
					//SCANNING
		            
					try {			
						
					//Timecode
					if (caseIncrementTimecode.isSelected())
					{
						FFPROBE.Data(file.toString());

						do {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {}
						} while (FFPROBE.isRunning);
					}
						
					String fichier = file.getName();
					lblEncodageEnCours.setText(fichier);
					
					//Dossier de sortie
					String sortie = setSortie("", file);
										
					final String extension =  fichier.substring(fichier.lastIndexOf("."));
					final String newExtension = comboFilter.getSelectedItem().toString();
					final String sortieFichier =  sortie + "/" + fichier.replace(extension, newExtension); 		
					
					//Mode concat
					String concat = FFMPEG.setConcat(file, sortie);					
					if (Settings.btnSetBab.isSelected() || VideoPlayer.comboMode.getSelectedItem().toString().equals(language.getProperty("removeMode")) && caseInAndOut.isSelected())
						file = new File(sortie.replace("\\", "/") + "/" + fichier.replace(extension, ".txt"));	
					else
						concat = " -noaccurate_seek";
						
					String audio = setAudio();
					String audioMapping = setAudioMapping();
					
					//InOut		
					FFMPEG.fonctionInOut();
					
	            	//Timecode
					String timecode = setTimecode();
					
					//Subtitles
					String subtitles = setSubtitles();
					
					//mapSubtitles
					String mapSubtitles = setMapSubtitles();
					
					//Si le fichier existe
					File fileOut = new File(sortieFichier);				
									
					if (fileOut.exists())
					{						
						fileOut = Utils.fileReplacement(sortie, fichier, extension, "_", newExtension);
						if (fileOut == null)
							continue;	
					}
									
					//Envoi de la commande
					String cmd = " -avoid_negative_ts make_zero -c:v copy -c:s copy" + audio + timecode + " -map v:0?" + audioMapping + mapSubtitles + " -y ";
					FFMPEG.run(FFMPEG.inPoint + concat + " -i " + '"' + file.toString() + '"' + subtitles + FFMPEG.outPoint + cmd + '"'  + fileOut + '"');		
					
					//Attente de la fin de FFMPEG
					do
							Thread.sleep(100);
					while(FFMPEG.runProcess.isAlive());
					
					if (FFMPEG.error)
					{
						//Envoi de la commande
						cmd = " -avoid_negative_ts make_zero -c:v copy" + audio + timecode + " -map 0:v:0?" + audioMapping + " -y ";
						FFMPEG.run(FFMPEG.inPoint + concat + " -i " + '"' + file.toString() + '"' + FFMPEG.outPoint + cmd + '"'  + fileOut + '"');		
						
						//Attente de la fin de FFMPEG
						do
								Thread.sleep(100);
						while(FFMPEG.runProcess.isAlive());
					}
					
					if (FFMPEG.saveCode == false && btnStart.getText().equals(Shutter.language.getProperty("btnAddToRender")) == false)
					{
						if (actionsDeFin(fichier, fileOut, sortie))
						break;
					}
					
				} catch (InterruptedException e) {
					FFMPEG.error  = true;
				}//End Try
			}//End For	
								
				if (btnStart.getText().equals(Shutter.language.getProperty("btnAddToRender")) == false)
					enfOfFunction();
			}//run
			
		});
		thread.start();
		
    }//main

	protected static String setAudio() {

		if (caseChangeAudioCodec.isSelected())
		{
			if (comboAudioCodec.getSelectedItem().toString().contains("PCM"))
			{
				switch (comboAudioCodec.getSelectedIndex()) 
				{
					case 0 :
						return " -c:a pcm_f32le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 1 :
						return " -c:a pcm_s32le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 2 :
						return " -c:a pcm_s24le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 3 :
						return " -c:a pcm_s16le -ar " + lbl48k.getText() + " -b:a 1536k";
				}
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("AAC"))
			{
				return " -c:a aac -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("AC3"))
			{
				return " -c:a ac3 -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("OPUS"))
			{
				return " -c:a libopus -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("OGG"))
			{
				return " -c:a libvorbis -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals(language.getProperty("noAudio")))
			{
				return " -an";
			}
		}

		return " -c:a copy";		
	}
	
	protected static String setAudioMapping() {
		String mapping = "";
		if (comboAudio1.getSelectedIndex() == 0
			&& comboAudio2.getSelectedIndex() == 1
			&& comboAudio3.getSelectedIndex() == 2
			&& comboAudio4.getSelectedIndex() == 3
			&& comboAudio5.getSelectedIndex() == 4
			&& comboAudio6.getSelectedIndex() == 5
			&& comboAudio7.getSelectedIndex() == 6
			&& comboAudio8.getSelectedIndex() == 7)
		{
			return " -map a?";	
		}
		else
		{
			if (comboAudio1.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio1.getSelectedIndex()) + "?";
			if (comboAudio2.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio2.getSelectedIndex()) + "?";
			if (comboAudio3.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio3.getSelectedIndex()) + "?";
			if (comboAudio4.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio4.getSelectedIndex()) + "?";
			if (comboAudio5.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio5.getSelectedIndex()) + "?";
			if (comboAudio6.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio6.getSelectedIndex()) + "?";
			if (comboAudio7.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio7.getSelectedIndex()) + "?";
			if (comboAudio8.getSelectedIndex() != 16)
				mapping += " -map a:" + (comboAudio8.getSelectedIndex()) + "?";
		}
		
		return mapping;
	}
	
	protected static String setTimecode() {
		if (caseSetTimecode.isSelected())
			return " -timecode " + TCset1.getText() + ":" + TCset2.getText() + ":" + TCset3.getText() + ":" + TCset4.getText();
	
		return "";
	}
	
	protected static String setSubtitles() {
		
		if (caseSubtitles.isSelected())
    	{			
    		return FFMPEG.inPoint + " -i " + '"' +  subtitlesFile.toString() + '"';
    	}
		
		return "";
	}
	
	protected static String setMapSubtitles() {
		
		if (caseSubtitles.isSelected())
		{
        	String[] languages = Locale.getISOLanguages();			
			Locale loc = new Locale(languages[comboSubtitles.getSelectedIndex()]);
			        	
        	if (comboFilter.getSelectedItem().toString().equals(".mkv"))
        		return " -map 1:s -c:s srt -metadata:s:s:0 language=" + loc.getISO3Language();
        	else
        		return " -map 1:s -c:s mov_text -metadata:s:s:0 language=" + loc.getISO3Language();      	
		}
		
		return " -map s?";
	}

	protected static String setSortie(String sortie, File file) {				
		if (caseChangeFolder1.isSelected())
			sortie = lblDestination1.getText();
		else
		{
			sortie =  file.getParent();
			lblDestination1.setText(sortie);
		}
		return sortie;
	}

	private static boolean actionsDeFin(String fichier, File fileOut, String sortie) {
		//Erreurs
		if (FFMPEG.error)
		{
			FFMPEG.errorList.append(fichier);
		    FFMPEG.errorList.append(System.lineSeparator());
			try {
				fileOut.delete();
			} catch (Exception e) {}
		}

		//Annulation
		if (cancelled)
		{
			try {
				fileOut.delete();
			} catch (Exception e) {}
			return true;
		}
		
		//Mode concat
		if (Settings.btnSetBab.isSelected() || VideoPlayer.comboMode.getSelectedItem().toString().equals(language.getProperty("removeMode")) && caseInAndOut.isSelected())
		{		
			final String extension =  fichier.substring(fichier.lastIndexOf("."));
			File listeBAB = new File(sortie.replace("\\", "/") + "/" + fichier.replace(extension, ".txt")); 			
			listeBAB.delete();
		}

		//Fichiers terminés
		if (cancelled == false && FFMPEG.error == false)
		{
			complete++;
			lblTermine.setText(Utils.completedFiles(complete));
		}
		
		//Ouverture du dossier
		if (caseOpenFolderAtEnd1.isSelected() && cancelled == false && FFMPEG.error == false)
		{
			try {
				Desktop.getDesktop().open(new File(sortie));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//Envoi par e-mail et FTP
		Utils.sendMail(fichier);
		Wetransfer.addFile(fileOut);
		Ftp.sendToFtp(fileOut);
		Utils.copyFile(fileOut);
		
		//Séquence d'images et bout à bout
		if (Settings.btnSetBab.isSelected())
			return true;
		
		//Timecode
		if (caseIncrementTimecode.isSelected())
		{			 
			NumberFormat formatter = new DecimalFormat("00");

			int timecodeToMs = Integer.parseInt(TCset1.getText()) * 3600000 + Integer.parseInt(TCset2.getText()) * 60000 + Integer.parseInt(TCset3.getText()) * 1000 + Integer.parseInt(TCset4.getText()) * (int) (1000 / FFPROBE.currentFPS);
			int millisecondsToTc = timecodeToMs + FFPROBE.totalLength;
			
			if (caseInAndOut.isSelected())
				millisecondsToTc = timecodeToMs + VideoPlayer.dureeHeures * 3600000 + VideoPlayer.dureeMinutes * 60000 + VideoPlayer.dureeSecondes * 1000 + VideoPlayer.dureeImages * (int) (1000 / FFPROBE.currentFPS);
			
			TCset1.setText(formatter.format(millisecondsToTc / 3600000));
			TCset2.setText(formatter.format((millisecondsToTc / 60000) % 60));
			TCset3.setText(formatter.format((millisecondsToTc / 1000) % 60));				        
			TCset4.setText(formatter.format((int) (millisecondsToTc / (1000 / FFPROBE.currentFPS) % FFPROBE.currentFPS)));	
		}
		
		//Scan
		if (Shutter.scanIsRunning)
		{
			Utils.moveScannedFiles(fichier);
			Rewrap.main();
			return true;
		}
		return false;
	}
}//Class
