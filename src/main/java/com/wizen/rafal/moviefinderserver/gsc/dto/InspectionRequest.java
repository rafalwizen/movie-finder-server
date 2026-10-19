package com.wizen.rafal.moviefinderserver.gsc.dto;

/**
 * Request body for the urlInspection/index:inspect endpoint.
 * Note: siteUrl goes raw (un-encoded) in the body here, unlike the
 * searchAnalytics endpoint where it is URL-encoded in the path.
 */
public record InspectionRequest(String inspectionUrl, String siteUrl, String languageCode) {
}
