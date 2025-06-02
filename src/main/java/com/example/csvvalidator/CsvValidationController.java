package com.example.csvvalidator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
public class CsvValidationController {

	@GetMapping("/")
	public String showUploadPage() {
		return "upload";
	}

	@PostMapping("/upload")
	public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {
		List<Map<String, String>> recordsList = new ArrayList<>();
		List<String> headers = Arrays.asList("センサID", "生データ", "補正係数", "上限", "下限", "補正後値", "Result");

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			CSVFormat format = CSVFormat.Builder.create()
					.setHeader()
					.setSkipHeaderRecord(true)
					.build();

			CSVParser parser = new CSVParser(reader, format);

			for (CSVRecord record : parser) {
				Map<String, String> row = new LinkedHashMap<>();
				record.toMap().forEach((k, v) -> {
					String key = k.trim();
					String value = (v != null ? v.trim() : "");
					row.put(key, value);
				});

				try {
					String rawStr = row.get("生データ");
					String coeffStr = row.get("補正係数");
					String upperStr = row.get("上限");
					String lowerStr = row.get("下限");

					if (rawStr != null && coeffStr != null && upperStr != null && lowerStr != null) {
						int raw = Integer.parseInt(rawStr);
						double coeff = Double.parseDouble(coeffStr);
						double upper = Double.parseDouble(upperStr);
						double lower = Double.parseDouble(lowerStr);

						double corrected = raw * coeff;
						row.put("補正後値", String.format("%.1f", corrected));
						row.put("Result", (corrected >= lower && corrected <= upper) ? "OK" : "NG");
					} else {
						row.put("補正後値", "N/A");
						row.put("Result", "NG");
					}
				} catch (Exception e) {
					row.put("補正後値", "N/A");
					row.put("Result", "NG");
				}

				recordsList.add(row);
			}

			model.addAttribute("headers", headers);
			model.addAttribute("records", recordsList);

		} catch (Exception e) {
			model.addAttribute("error", "CSVの読み込みに失敗しました: " + e.getMessage());
		}

		return "result";
	}
}
