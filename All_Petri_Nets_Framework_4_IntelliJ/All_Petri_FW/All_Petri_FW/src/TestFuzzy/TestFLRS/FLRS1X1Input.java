package TestFuzzy.TestFLRS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class FLRS1X1Input {
	public static void main(String[] args) throws InterruptedException, IOException {
		File file = new File("/Users/dimo/Library/CloudStorage/GoogleDrive-andreyyt1234@gmail.com/My Drive/University/Year 4/Year 4 Semester 1/Distributed Control Systems/Project/DCS_Project/test3.txt");
		Files.deleteIfExists(file.toPath());
		FileWriter fw = new FileWriter(file.getPath());
		Float f = -1f;
		for (int i = 0; i < 100; i++) {

			fw.write("P1:"+f+"F\n");
			f += 0.1f;
		}
		fw.close();
		System.out.println("Done!");
	}
}