package com.wizen.rafal.moviefinderserver.gsc.service;

import com.wizen.rafal.moviefinderserver.gsc.dto.GscReportData;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Renders the report as Polish-language Telegram HTML. Only tags supported
 * by Telegram parse_mode=HTML (b, a, i) are emitted, and every dynamic value
 * is escaped before embedding.
 */
@Component
public class GscReportFormatter {

	private static final Locale PL = Locale.of("pl", "PL");

	public String format(GscReportData data, String siteUrl) {
		StringBuilder sb = new StringBuilder();

		sb.append("<b>📊 Raport GSC — ").append(escapeHtml(host(siteUrl))).append("</b>\n");
		sb.append("Okres: ").append(data.current().start()).append(" → ").append(data.current().end())
				.append(" (poprzedni: ").append(data.previous().start()).append(" → ").append(data.previous().end()).append(")\n\n");

		appendTotals(sb, data);
		appendTops(sb, "🔎", "fraz", data.topQueries(), false);
		appendTops(sb, "📄", "podstron", data.topPages(), true);
		appendInspections(sb, data.inspections());

		sb.append("<i>Dane GSC dojrzewają ok. 2–3 dni; okno raportu kończy się ").append(data.current().end()).append(".</i>");
		return sb.toString();
	}

	public static String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void appendTotals(StringBuilder sb, GscReportData data) {
		GscReportData.WindowTotals current = data.current();
		GscReportData.WindowTotals previous = data.previous();
		long days = ChronoUnit.DAYS.between(current.start(), current.end()) + 1;

		sb.append("<b>Wyniki z wyszukiwarki (").append(days).append(" dni)</b>\n");
		sb.append("• Kliknięcia: <b>").append(intPl(current.clicks())).append("</b> ")
				.append(percentDelta(current.clicks(), previous.clicks())).append('\n');
		sb.append("• Wyświetlenia: <b>").append(intPl(current.impressions())).append("</b> ")
				.append(percentDelta(current.impressions(), previous.impressions())).append('\n');
		sb.append("• Średni CTR: <b>").append(pctPl(current.ctr())).append("</b> ")
				.append(ppDelta(current.ctr(), previous.ctr())).append('\n');
		sb.append("• Średnia pozycja: <b>").append(dec1Pl(current.position())).append("</b> ")
				.append(positionDelta(current.position(), previous.position())).append("\n\n");
	}

	private void appendTops(StringBuilder sb, String emoji, String label, List<GscReportData.TopEntry> entries, boolean asLinks) {
		sb.append("<b>").append(emoji).append(" Top ").append(entries.size()).append(" ").append(label).append("</b>\n");
		if (entries.isEmpty()) {
			sb.append("— brak danych —\n\n");
			return;
		}
		int index = 1;
		for (GscReportData.TopEntry entry : entries) {
			sb.append(index++).append(". ");
			if (asLinks) {
				sb.append("<a href=\"").append(escapeHtml(entry.key())).append("\">")
						.append(escapeHtml(displayPath(entry.key()))).append("</a>");
			} else {
				sb.append(escapeHtml(entry.key()));
			}
			sb.append(" — ").append(intPl(entry.clicks())).append(" klik. / ")
					.append(intPl(entry.impressions())).append(" wyśw. / poz. ")
					.append(dec1Pl(entry.position())).append('\n');
		}
		sb.append('\n');
	}

	private void appendInspections(StringBuilder sb, List<GscReportData.InspectionSummary> inspections) {
		sb.append("<b>🔍 Indeksowanie</b>\n");
		for (GscReportData.InspectionSummary item : inspections) {
			sb.append("• <a href=\"").append(escapeHtml(item.url())).append("\">")
					.append(escapeHtml(displayPath(item.url()))).append("</a> — ");
			if (item.error() != null) {
				sb.append("❓ nieznany (błąd: ").append(escapeHtml(item.error())).append(')');
			} else {
				sb.append(verdictLabel(item.verdict()));
				if (item.coverageState() != null && !item.coverageState().isBlank()) {
					sb.append(" (").append(escapeHtml(item.coverageState())).append(')');
				}
				if (item.lastCrawlTime() != null && !item.lastCrawlTime().isBlank()) {
					sb.append(", crawl: ").append(crawlDate(item.lastCrawlTime()));
				}
			}
			sb.append('\n');
		}
		sb.append('\n');
	}

	private String verdictLabel(String verdict) {
		return switch (verdict == null ? "" : verdict) {
			case "PASS" -> "✅ Zaindeksowana";
			case "NEUTRAL" -> "⏸ Wykluczona z indeksu";
			case "FAIL" -> "❌ Błąd";
			default -> "❓ Nieznany";
		};
	}

	private String crawlDate(String lastCrawlTime) {
		try {
			return OffsetDateTime.parse(lastCrawlTime).toLocalDate().toString();
		} catch (RuntimeException e) {
			return escapeHtml(lastCrawlTime);
		}
	}

	private String percentDelta(long current, long previous) {
		if (previous == 0) {
			return current == 0 ? "(bez zmian)" : "(nowość — wcześniej 0)";
		}
		double delta = (current - previous) * 100.0 / previous;
		String arrow = delta > 0 ? "▲" : delta < 0 ? "▼" : "▬";
		return "(" + arrow + " " + signed1Pl(delta) + "%)";
	}

	private String ppDelta(double current, double previous) {
		double delta = (current - previous) * 100.0; // ctr values are 0-1 fractions
		if (Math.abs(delta) < 0.005) {
			return "(bez zmian)";
		}
		String arrow = delta > 0 ? "▲" : "▼";
		return "(" + arrow + " " + signed2Pl(delta) + " p.p.)";
	}

	private String positionDelta(double current, double previous) {
		double improvement = previous - current; // positive = better (lower position)
		if (Math.abs(improvement) < 0.05) {
			return "(bez zmian)";
		}
		return improvement > 0
				? "(o " + dec2Pl(improvement) + " lepiej)"
				: "(o " + dec2Pl(-improvement) + " gorzej)";
	}

	private String host(String siteUrl) {
		try {
			return java.net.URI.create(siteUrl).getHost();
		} catch (RuntimeException e) {
			return siteUrl;
		}
	}

	private String displayPath(String url) {
		if (url.startsWith("https://")) {
			return url.substring("https://".length());
		}
		if (url.startsWith("http://")) {
			return url.substring("http://".length());
		}
		return url;
	}

	private String intPl(long value) {
		return NumberFormat.getIntegerInstance(PL).format(value);
	}

	private String dec1Pl(double value) {
		return String.format(PL, "%.1f", value);
	}

	private String dec2Pl(double value) {
		return String.format(PL, "%.2f", value);
	}

	private String pctPl(double fraction) {
		return String.format(PL, "%.2f%%", fraction * 100);
	}

	private String signed1Pl(double value) {
		return (value > 0 ? "+" : "") + String.format(PL, "%.1f", value);
	}

	private String signed2Pl(double value) {
		return (value > 0 ? "+" : "") + String.format(PL, "%.2f", value);
	}
}
