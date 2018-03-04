all: sshpc.c thpool.c
	gcc sshpc.c thpool.c -pthread -o sshpc -lssl -lcrypto
	gcc encrypt.c -o encrypt -lssl -lcrypto
clean:
	rm encrypt
	rm sshpc
