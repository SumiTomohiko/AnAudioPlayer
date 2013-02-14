
An Audio Player
***************

An Audio Player (AAP) is an audio player for Android.

Current version has many critical bugs. READ "Known problems and bugs" SECTION
BEFORE USING.

Requirements
============

AAP works on::

* Android 3.0 (API level 11) or later

The author tested AAP on Acer A500. Features of A500 are::

* Android 3.2.1 (API level 13)
* 10inch (1280x800) screen

License
=======

AAP is an open source software on MIT lisence. See COPYING.rst.

How to build
============

AAP can be built on::

* Eclipse Juno Service Release 1
* JDK 1.7.0

Known problems and bugs
=======================

1.  AAP cannot handle tablet's rotation completely. If a user rotates his/her
    tablet, some information will be lost.
2.  There are some critical bugs. The author does not know them still strictly
    (the author guesses that they are problems around activity's lifecycle).
3.  The player does not show timestamp in sliding the record or the head.
4.  When a user comes back to the directory/file list, the list does not show
    which entry is selected by pink background.
5.  AAP cannot handle portrait mode well. AAP must have a special layout for
    portrait mode.
6.  Staff icon is not cool.
7.  Buttons' background is not cool.
8.  The arm's curve is not cool. Radius must be larger.

Author
======

The author is `Tomohiko Sumi
<http://neko-daisuki.ddo.jp/~SumiTomohiko/index.html>`.

.. vim: tabstop=2 shiftwidth=2 expandtab softtabstop=2 filetype=rst
