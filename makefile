.ONESHELL:
setup:
	cd ./bin
	unzip babashka-0.1.3-linux-static-amd64.zip
	unzip pod-babashka-hsqldb-0.0.1-linux-amd64.zip
	cd ../lib
	unzip libs.zip

.ONESHELL:
clean:
	cd ./bin
	find . -type f -not -name '*.zip' -delete
	cd ../lib
	find . -type f -not -name '*.zip' -delete
