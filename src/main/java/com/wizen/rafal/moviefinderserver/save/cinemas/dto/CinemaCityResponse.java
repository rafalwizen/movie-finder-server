package com.wizen.rafal.moviefinderserver.save.cinemas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinemaCityResponse {

	private Body body;

	public Body getBody() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}

	@Setter
	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Body {
		private List<CinemaDto> cinemas;

	}

	@Setter
	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CinemaDto {

		private String id;

		@JsonProperty("displayName")
		private String displayName;

		private String link;

		@JsonProperty("addressInfo")
		private AddressInfo addressInfo;

		private Double latitude;

		private Double longitude;

	}

	@Setter
	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class AddressInfo {

		private String address1;

		private String city;

		@JsonProperty("postalCode")
		private String postalCode;

		public String getFullAddress() {
			StringBuilder sb = new StringBuilder();
			if (address1 != null && !address1.isEmpty()) {
				sb.append(address1);
			}
			if (postalCode != null && !postalCode.isEmpty()) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(postalCode);
			}
			if (city != null && !city.isEmpty()) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(city);
			}
			return sb.toString();
		}
	}
}
