Changelog
=========

Version 1.3 (2017-03-04)
----------------------------

* Compatibility with all gradle versions >= 2.3
* Update gradle wrapper to 3.4
* Better multi-project handling:
   - Applying the plugin on the root project adds the taskTree task to all child projects.
   - Applying the plugin under `allrojects`  or `subprojects` exhibits the same behavior (and does not fail anymore due to `task taskTree is already defined`)

