#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <openssl/des.h>

int main(int argc, char *argv[]) {
	char* data = malloc(sizeof(char) * 14);

	DES_fcrypt(argv[1],"ic",data);
	printf("%s\n",data);
	return 0;
}