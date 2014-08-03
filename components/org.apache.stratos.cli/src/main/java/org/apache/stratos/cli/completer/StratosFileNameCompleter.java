package org.apache.stratos.cli.completer;

import java.io.File;
import java.util.List;

import org.apache.stratos.cli.utils.CliConstants;

import jline.console.completer.FileNameCompleter;

public class StratosFileNameCompleter extends FileNameCompleter {

	@Override
	public int complete(String buf, int arg1, List<CharSequence> candidates) {

		String buffer = (buf == null) ? "" : buf;
		String subString = null;
		int index = buf.lastIndexOf("--"+CliConstants.RESOURCE_PATH_LONG_OPTION);
		if (buf.length() >= index + 16) {
			subString = buf.substring(index + 16);
		}

		String translated = (subString == null || subString.isEmpty()) ? buf
				: subString;
		if (translated.startsWith("~" + File.separator)) {
			translated = System.getProperty("user.home")
					+ translated.substring(1);
		} else if (translated.startsWith("." + File.separator)) {
			translated = new File("").getAbsolutePath() + File.separator
					+ translated;
		}

		File f = new File(translated);

		final File dir;

		if (translated.endsWith(File.separator)) {
			dir = f;
		} else {
			dir = f.getParentFile();
		}

		final File[] entries = (dir == null) ? new File[0] : dir.listFiles();

		return matchFiles(buffer, translated, entries, candidates);

	}

}
