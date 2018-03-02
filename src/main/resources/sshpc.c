#define _GNU_SOURCE
#define HASH_PSWD_SIZE 15
#define NUM_CORES 4

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <pthread.h>
#include <time.h>
#include <openssl/des.h>
#include "thpool.h"


char *xxx = "!_!";



void release(char* start,char * end,char *pswd_job ) {
	free(start);
	free(end);
	free(pswd_job);
	
}
/*
char * request_client() {
	char * pswd_job;
	pswd_job = malloc  (HASH_PSWD_SIZE * 100 * sizeof(char));
	strcpy(pswd_job,crypt("AAAA1234","ic"));
	return pswd_job;
}
*/


char ** split_job(char *start,char *end) {
	
	char arr[10] = {'A','G','O','Y','g','o','y','4','\0'};
	int index = 4;
	char ** ret = malloc(sizeof(char) *100 *10);
	for (int j = 0;j < (8);j++) {
		char *sub_start; 
		sub_start = malloc(sizeof(char) * 12);
		char *sub_end; 
		sub_end = malloc(sizeof(char) * 12);
		for (int k = 0; k < 8;k++) {
			if (k == index) {
			    sub_start[k] = arr[j];
				sub_end[k] = arr[j+1];
			 }
			else { 
				sub_start[k] = start[k]; 
				sub_end[k] = start[k];	
			}
		}
		//printf("split_job: %s",sub_start);
		ret[j*2] = sub_start; 
		ret[j*2+1] = sub_end;
		}

	ret[15] = end;
	return ret;
	
}

void * do_work(char ** arg_arr ) {
	char* start = arg_arr[0];
	char * end = arg_arr[1];
	char *pswd_job = arg_arr[2];
	int lsb = strlen(start) -1;
	int index = lsb;
	char digit;
    char *hashed_start;
    //printf("starting work on -> start: %s, end: %s & pointer to start: %p pointer to end: %p\n",start,end, start,end);
  	int str_len_hash = strlen(pswd_job);
  	int str_len_start = strlen(start);
	char* data = malloc(sizeof(char) * 15);
  	int count = 0;
	while (strcmp(start,end)) {
		hashed_start = DES_fcrypt(start,"ic",data);
		if (!strncmp(hashed_start,pswd_job,str_len_start)) {
			xxx = malloc (sizeof(char) *15 );
			strcpy(xxx,start);
			break;
		}
		
    	if (start[index] == 'Z') { start[index] = 'a'; index = lsb;}
    	else if (start[index] == 'z') { start[index] = '0';  index = lsb;} 
    	else if (start[index] != '9') { start[index]++; index = lsb; }
    	else {
    		start[index] = 'A';
    		index--;
		}

		 count++;
	}
	free(data);
	//printf("Done-> start :%s, end: %s & pointer to start: %p, pointer to end: %p\n",start, end,start,end);
	return (void*) 1;
	
  }



void worker(char* start,char * end,char *pswd_job ) {
	threadpool thpool = thpool_init(8);
	char ** arr = split_job(start,end);
	char *** matrix = malloc(90*100*sizeof(char));
	int n = sizeof(arr);
	
	for (int i = 0;i < 8;i++) {
		char ** main_arr = malloc(90*100*sizeof(char));
		//printf("start:%s  &&  end: %s, i*2: %d , (i*2 +1): %d\n",arr[i*2],arr[i*2+1],i*2,(i*2)+1);
		main_arr[0] = arr[i*2];
		main_arr[1] = arr[i*2 +1];
		main_arr[2] = pswd_job;
		matrix[i] = main_arr;
		thpool_add_work(thpool, (void*)do_work, matrix[i]);
		
	}
	thpool_wait(thpool);
	thpool_destroy(thpool);

}
/*
void dispatcher(char *pswd_job) {

	char * start;
	char * end;
	start = malloc( HASH_PSWD_SIZE* sizeof(char) );
	end = malloc( HASH_PSWD_SIZE * sizeof(char) );
	strcpy(start,"AAAAAAAA");
	strcpy(end,"AAABAAAA");
	worker(start,end,pswd_job);
}
*/

int main(int argc, char *argv[]) {

	char * start = malloc( HASH_PSWD_SIZE* sizeof(char) );
	char * end = malloc( HASH_PSWD_SIZE * sizeof(char) );
	char * pswd_job = malloc  (HASH_PSWD_SIZE * sizeof(char));
	strcpy(pswd_job,argv[1]);
	strcpy(start,argv[2]);
	strcpy(end,argv[3]);
	//char * faked_job = request_client();
	worker(start,end,pswd_job);

	printf("%s", xxx);
    return 0;

  	

}






