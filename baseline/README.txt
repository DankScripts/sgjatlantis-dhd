CONFIRMED WORKING BASELINE

R17b was confirmed by the user on 2026-08-27 to work completely and is now the
permanent full-project baseline. Its exact JAR, producing source package,
checksums, and preservation contract are stored under:

confirmed-working-r17b/

The accepted R13 GUI is independently frozen under accepted-gui-r13/ and must
remain unchanged unless the user explicitly requests a GUI change.

RELEASE FOUNDATION

The confirmed R17b JAR is the direct release foundation. The release task
preserves its entries and replaces only the manifest, Forge metadata, resource
pack description, and exact-version network class when producing a requested
reversioned build.

HISTORICAL BUILD FOUNDATION

The frozen 2.0.0-beta.1 uniform-table-background JAR remains the binary build
foundation for the earlier compatibility development that led to R17b. Its
SHA-256 is:

315f87cda1e2c7b5939a5c7efdf2c5bec363427b6cd0092327e753ab9af9b8fd

A technically successful build is not a new accepted baseline until the user
confirms it.
