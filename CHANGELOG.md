Changelog
=========

This changelog format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Version 2.0 (2021-07-01)
----------------------------
* Support Gradle 6.8 and make it the minimum supported Gradle version
* Add options `--with-inputs` and `--with-outputs` to print task inputs and outputs
* Change option `--task-depth` to `--depth`
* Change default behavior to _not_ repeat an already printed sub-tree. Option `--no-repeat` replaced with `--repeat`.

Version 1.5 (2020-01-01)
----------------------------
* Compatibility with gradle 6

Version 1.4 (2019-06-01)
----------------------------
* Add `--task-depth` option to limit tree depth.
* Fix bug: `No such property task for class TransformInfo$ChainedTransformInfo`

Version 1.3.1 (2018-10-08)
----------------------------

* Compatibility with gradle version 5.0-milestone-1

Version 1.3 (2017-03-04)
----------------------------

* Compatibility with all gradle versions >= 2.3
* Update gradle wrapper to 3.4
* Better multi-project handling:
   - Applying the plugin on the root project adds the taskTree task to all child projects.
   - Applying the plugin under `allrojects`  or `subprojects` exhibits the same behavior (and does not fail anymore due to `task taskTree is already defined`)

