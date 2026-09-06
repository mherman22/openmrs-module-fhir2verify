package org.openmrs.module.fhir2verify;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.openmrs.module.fhir2.api.annotations.FhirInterceptor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("failingVerifyInterceptor")
@Lazy
@FhirInterceptor
@Interceptor
public class FailingVerifyInterceptor {

	public FailingVerifyInterceptor() {
		throw new IllegalStateException("FHIR2VERIFY-FAILING-BEAN cannot be constructed");
	}

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public boolean neverRuns(HttpServletRequest request, HttpServletResponse response) {
		return true;
	}
}
