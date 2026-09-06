package org.openmrs.module.fhir2verify;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.openmrs.module.fhir2.api.annotations.FhirInterceptor;
import org.springframework.stereotype.Component;

@Component("healthyVerifyInterceptor")
@FhirInterceptor
@Interceptor
public class HealthyVerifyInterceptor {

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public boolean stamp(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("X-Verify-Contributed", "1");
		System.out.println("FHIR2VERIFY-HEALTHY-DISPATCHED uri=" + request.getRequestURI());
		return true;
	}
}
