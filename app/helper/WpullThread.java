package helper;

import java.io.File;
import java.lang.Process;
import java.lang.ProcessBuilder;
import models.Gatherconf;
import play.Logger;

/**
 * @author I. Kuss Ein Thread, in dem ein Webcrawl gestartet wird. Der Thread
 *         wartet, bis der Crawl beendet ist. Ist der Crawl mit Fehler beendet,
 *         wird ein neuer Thread aufgerufen, der einen erneuten Crawl-Versuch
 *         macht. Es gibt eine Obergrenze für die Anzahl Crawl-Versuche:
 *         maxNumberAttempts.
 */
public class WpullThread extends Thread {

	private Gatherconf conf = null;
	private File crawlDir = null;
	private String warcFilename = null;
	private ProcessBuilder pb = null;
	private File logFile = null;
	private int exitState = 0;
	/**
	 * Der wievielte Versuch ist es, diesen Crawl zu starten ?
	 */
	int attempt = 1;

	private static int maxNumberAttempts = 10;
	private static final Logger.ALogger WebgatherLogger =
			Logger.of("webgatherer");

	/**
	 * Der Konstruktor für diese Klasse.
	 * 
	 * @param pb Ein Objekt der Klasse ProcessBuilder mit Aufrufinformationen für
	 *          den Crawl.
	 * @param attempt Der wievielte Versuch es ist, diesen Webschnitt zu sammeln.
	 */
	public WpullThread(ProcessBuilder pb, int attempt) {
		this.pb = pb;
		this.attempt = attempt;
		exitState = 0;
	}

	/**
	 * Die Set-Methode für den Parameter conf
	 * 
	 * @param conf Die Gatherconf der Website, die gecrawlt werden soll.
	 */
	public void setConf(Gatherconf conf) {
		this.conf = conf;
	}

	/**
	 * Die Methode, um den Parameter crawlDir zu setzen.
	 * 
	 * @param crawlDir Das Verzeichnis (absoluter Pfad), in das wpull seine
	 *          Ergebnisdateien (z.B. WARC-Datei) schreibt.
	 */
	public void setCrawlDir(File crawlDir) {
		this.crawlDir = crawlDir;
	}

	/**
	 * Die Methode, um den Parameter warcFilename zu setzen.
	 * 
	 * @param warcFilename Der Dateiname (kein Pfad, auch keine Endung !) für die
	 *          WARC-Datei.
	 */
	public void setWarcFilename(String warcFilename) {
		this.warcFilename = warcFilename;
	}

	/**
	 * Die Methode, um den Parameter logFile zu setzen.
	 * 
	 * @param logFile Die Logdatei für wpull (crawl.log). Objekttyp "File".
	 */
	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	/**
	 * Die Methode, um exit State auszulesen
	 * 
	 * @return exitState ist der Return-Wert von wpull.
	 */
	public int getExitState() {
		return this.exitState;
	}

	/**
	 * This methods starts a webcrawl and waits for completion.
	 */
	@Override
	public void run() {
		try {
			Process proc = pb.start();
			assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
			assert pb.redirectOutput().file() == logFile;
			assert proc.getInputStream().read() == -1;
			exitState = proc.waitFor();
			/**
			 * Exit-Status: 0 = Crawl erfolgreich beendet
			 */
			WebgatherLogger.info("Webcrawl for " + conf.getName()
					+ " exited with exitState " + exitState);
			if (exitState == 0) {
				return;
			}
			// Keep warc file of failed crawl
			File warcFile =
					new File(crawlDir.toString() + "/" + warcFilename + ".warc.gz");
			File warcFileAttempted = new File(crawlDir.toString() + "/" + warcFilename
					+ ".warc.gz.attempt" + attempt);
			warcFile.renameTo(warcFileAttempted);
			warcFile.delete();
			attempt++;
			if (attempt > maxNumberAttempts) {
				throw new RuntimeException("Webcrawl für " + conf.getName()
						+ " fehlgeschlagen: Maximale Anzahl Versuche überschritten !");
			}
			// Crawl wird erneut angestoßen
			WebgatherLogger.info("Webcrawl for " + conf.getName()
					+ " wird erneut angestoßen. " + attempt + ". Versuch.");
			WpullThread wpullThread = new WpullThread(pb, attempt);
			wpullThread.setConf(conf);
			wpullThread.setCrawlDir(crawlDir);
			wpullThread.setWarcFilename(warcFilename);
			wpullThread.setLogFile(logFile);
			wpullThread.start();
		} catch (Exception e) {
			WebgatherLogger.error(e.toString());
			throw new RuntimeException("wpull crawl not successfully started!", e);
		}
	}

}