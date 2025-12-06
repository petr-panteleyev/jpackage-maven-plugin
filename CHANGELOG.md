# Changelog

## [1.7.1] - 2025-09-27

### Changed
- Updated documentation and examples
- Updated dependencies

## [1.7.0] - 2025-09-22

### Changed
- `name` and `destination` parameters made mandatory

### Removed
- Parameters version check
- Obsolete jpackage parameters: `bindServices`, `macBundleSigningPrefix`

## [1.6.6] - 2025-02-07

### Added
- Option: `removeDestination`

## [1.6.5] - 2024-06-10

### Added
- Support for more version strings
- Maven wrapper

## [1.6.4] - 2024-05-26

### Changed
- Plugin dependencies
- Documentation

## [1.6.3] - 2024-03-08

### Changed
- `installDir` as String

## [1.6.2] - 2024-03-08

### Fixed
- Plugin build dependencies

## [1.6.1] - 2024-03-08

### Changed
- `installDir` parameter is now passed as is without transforming into the absolute path

## [1.6.0] - 2022-11-04

### Added
- Introduced jpackage version check for parameters
- New jpackage options:
  - --jlink-options
  - --linux-package-deps
  - --bind-services
  - --about-url
  - --mac-app-store
  - --mac-entitlements
  - --mac-app-category
  - --win-help-url
  - --win-shortcut-prompt
  - --win-update-url
  - --app-content
  - --mac-dmg-content
  - --launcher-as-service

## [1.5.2] - 2022-03-30

### Added
- `skip` parameter

## [1.5.1] - 2021-04-29

### Changed
- Using org.apache.maven.shared.utils.cli

## [1.5.0] - 2021-04-10

### Added
- --app-image
- Support for additional jpackage options

### Removed
- Single `modulePath` parameter
- `JPACKAGE_HOME`

## [1.4.0] - 2021-01-31

### Added
- `addModules` parameter
- Multiple `modulePath` parameters
- Default value for `name` parameter

### Removed
- Custom path resolution

### Deprecated
- Single `modulePath` parameter
- `JPACKAGE_HOME`

## [1.3.1] - 2021-01-30

### Added
- Dry run mode

## [1.3.0] - 2021-01-24

### Added
- --license-file
- --file-associations
- --add-launcher
- --win-console

## [1.2.2] - 2020-12-28

### Changed
- `@Execution` annotation removed from Mojo
- Documentation updates

## [1.2.1] - 2020-11-04

### Added
- `version` default value added to parameter definition
- Plugin documentation site

## [1.2.0] - 2020-10-12

### Added
- `JPACKAGE_HOME` support to override configured toolchain or `java.home`

## [1.1.0] - 2020-10-02

### Added
- Support for jpackage --resource-dir option
- Support for jpackage --temp option

### Changed
- Main-jar option needs to be a relative path

### Other
- Dependency updates

## [1.0.1] - 2020-08-20

### Added
- Adding `.exe` when using `java.home` on Windows

## [1.0.0] - 2020-08-09

### Added
- Absolute path expansion for relative directory/file parameters

### Fixed
- jpackage output redirection fixed

## [0.0.4] - 2020-06-29

### Added
- --verbose
- --install-dir
- --arguments
- Proper escaping for jvm options
- Fallback to `java.home` if toolchain is not configured

## [0.0.3] - 2020-06-18

### Added
- Linux specific parameters

## [0.0.2] - 2020-06-09

### Added
- OS X specific parameters

## [0.0.1] - 2020-05-19

### Added
- Initial support for OS X and Windows
