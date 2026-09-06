# fhir2verify

A throwaway OpenMRS module that exists to be observed, not used. It contributes two
`@FhirInterceptor` beans to the FHIR2 module's interceptor extension point: one that works and one
that cannot be constructed. Deploy it next to the module under test and the difference between "one
bad bean costs itself" and "one bad bean costs everybody" becomes a response header you can `curl`.

Built for [openmrs-module-fhir2#628](https://github.com/openmrs/openmrs-module-fhir2/pull/628)
(FM2-698), which adds the `@FhirInterceptor` extension point.

## What is in it

| bean | what it does |
|---|---|
| `HealthyVerifyInterceptor` | Hooks `SERVER_INCOMING_REQUEST_PRE_PROCESSED` and sets `X-Verify-Contributed: 1` on the response. |
| `FailingVerifyInterceptor` | `@Lazy`, and its constructor throws unconditionally. |

Two details carry the whole design.

The healthy bean's signal is a **response header rather than a log line**. Whether a contributed
interceptor is registered is otherwise only visible in the server log, at a level OpenMRS does not
enable by default, which makes "it is registered" a thing you take on trust. A header is on the wire.

The failing bean is **`@Lazy` on purpose**. An eager singleton whose constructor throws fails the
Spring context refresh itself, long before the FHIR servlet is reached — a different failure that
tells you nothing about interceptor registration. `@Lazy` defers the throw to the moment the servlet
asks for the bean, which is the moment under test.

## Using it

```bash
mvn clean package        # -> target/fhir2verify-1.0.0.omod
```

Drop the omod in with the rest of the modules, start the server, then:

```bash
curl -i -u admin:Admin123 'http://localhost:8080/openmrs/ws/fhir2/R4/Patient?_count=1'
```

`X-Verify-Contributed: 1` on that response means the healthy bean is registered and dispatching,
despite its unbuildable sibling. Absent means it is not.

Two more probes are worth running, because they check the guarantees either side of the one above:

```bash
curl -i 'http://localhost:8080/openmrs/ws/fhir2/R4/Patient'    # 401, and NO header: authentication
curl -i 'http://localhost:8080/openmrs/ws/fhir2/R4/metadata'   # 200, WITH the header: exempt path
```

The 401 carrying no header is the interesting one: it shows the built-in authentication interceptor
still dispatches ahead of contributed beans, so a contributed interceptor never sees an
unauthenticated request.

To exercise re-registration rather than first registration, force an OpenMRS context refresh by
restarting any unrelated started module, then repeat the probes:

```bash
curl -i -u admin:Admin123 -X POST -H 'Content-Type: application/json' \
  -d '{"action":"restart","modules":["<some other started module>"]}' \
  'http://localhost:8080/openmrs/ws/rest/v1/moduleaction'
```

## What it showed

Against a real OpenMRS 3 backend, with fhir2 read back over REST as the build under test rather than
inferred from a filename:

- **Before the fix**, no `X-Verify-Contributed` header at all, and one log line:
  `Could not read the contributed FHIR interceptors from the Spring context; none of them will run`.
  The healthy bean never ran, because the servlet resolved all annotated beans in a single
  `getBeansWithAnnotation` call that threw as a unit.
- **After the fix**, the header is present, survives a context refresh, and the log names only the
  bean that actually failed.

Swapping only the fhir2 omod back and forth reproduced both states, so the difference is attributable
to that artefact alone.

The same rig was then run alongside the SMART on FHIR module, whose `SmartScopeInterceptor` was made
to fail on reconstruction. After the refresh the log named `smartScopeInterceptor` and
`failingVerifyInterceptor` as unbuildable, never named `healthyVerifyInterceptor`, and
`X-Verify-Contributed: 1` was still on the wire: one module's broken interceptor no longer takes
another module's with it.

## Do not ship this

It is deliberately broken, it prints to stdout, and its failing bean logs an error on every context
refresh for as long as it is installed.
