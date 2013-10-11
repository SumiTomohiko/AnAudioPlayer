
ANT=	ant

all:
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
