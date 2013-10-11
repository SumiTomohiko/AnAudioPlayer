
ANT=	ant

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

.PHONY: icon
