
ANT=		ant
PKG_DIR=	src/jp/gr/java_conf/neko_daisuki
PKG=		jp.gr.java_conf.neko_daisuki

all: apk

apk:
	@$(ANT)

release:
	@$(ANT) release

install:
	@$(ANT) installd

clean:
	@$(ANT) clean

icon:
	@$(ANT) icon

prepare:
	@rm -f $(PKG).anaudioplayer
	@ln -fs $(PKG_DIR)/anaudioplayer $(PKG).anaudioplayer
	@rm -f $(PKG).android.widget
	@ln -fs $(PKG_DIR)/android/widget $(PKG).android.widget

.PHONY: icon
