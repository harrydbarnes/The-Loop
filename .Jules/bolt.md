## 2024-05-22 - [Kapt Environment Failure]
**Learning:** The build environment consistently fails with `IllegalAccessError` in `KaptJavaCompiler` due to JDK compatibility issues, preventing `testDebugUnitTest` and `lintDebug` from running successfully.
**Action:** When working in this environment, rely on rigorous manual code review and syntax checking when Kapt-dependent tasks fail, and document the limitation. Prioritize changes that don't require deep Kapt processing if possible, or verify logic in isolation if feasible.
