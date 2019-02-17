// PatternRecognize.cpp : 定義主控台應用程式的進入點。
//
#include "stdafx.h"
#include <bitset>
//#include <experimental/filesystem>
#include <windows.h>
#include <iostream>

#include <limits>

std::vector<std::string>  getAllFilesNamesWithinFolder(std::string folder)
{
	std::vector<std::string> names;
	std::string search_path = folder + "/*.*";
	WIN32_FIND_DATAA fd;

	HANDLE hFind = ::FindFirstFileA(search_path.c_str(), &fd);
	if (hFind != INVALID_HANDLE_VALUE) {
		do {
			// read all (real) files in current folder
			// , delete '!' read other 2 default folder . and ..
			if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
				names.push_back(fd.cFileName);
			}
		} while (::FindNextFileA(hFind, &fd));
		::FindClose(hFind);
	}
	return names;
}

std::string lowerCase(std::string input) {
	for (std::string::iterator it = input.begin(); it != input.end(); ++it)
		*it = tolower(*it);
	return input;
}

std::vector<std::string> getAllImageFileNamesWithinFolder(std::string folder)
{
	using namespace std;
	auto files = getAllFilesNamesWithinFolder(folder);
	vector<std::string> result;

	for (auto file : files)
	{
		auto lower = lowerCase(file);

		if (string::npos != lower.rfind(".bmp") || string::npos != lower.rfind(".png") || string::npos != lower.rfind(".jpg") || string::npos != lower.rfind(".jpeg"))
		{
			//cout << file << endl;
			result.push_back(file);
		}

	}
	return result;
}

#define IMAGE_LENGTH 8 //比5小, 這個方法就分不出來了
class Image {
public:
	bool content[IMAGE_LENGTH*IMAGE_LENGTH];
	unsigned long long  mask = 0;
	cv::Mat binary_image;
	Image() {

	}
};

unsigned long get_SAD(Image img0, Image img1) {
	unsigned long sad = 0;
	for (int x = 0; x < IMAGE_LENGTH*IMAGE_LENGTH; x++) {
		sad += (img0.content[x] != img1.content[x]) ? 1 : 0;
	}
	return sad;
}

void to_binary_image(cv::Mat image) {
	int height = image.rows;
	int width = image.cols;

	//int min_value = 255;
	//for (int h = 0; h < height; h++) {
	//	for (int w = 0; w < width; w++) {
	//		auto& pixel = image.at<cv::Vec3b>(w, h);
	//		min_value = min(pixel[1], min_value);
	//	}
	//}

	for (int h = 0; h < height; h++) {
		for (int w = 0; w < width; w++) {
			auto& pixel = image.at<cv::Vec3b>(w, h);
			if (pixel[1] ==255) {
				pixel[2] = pixel[1] = pixel[0] = 255;
			}
			else {
				pixel[2] = pixel[1] = pixel[0] = 0;
			}

			int a = 1;
		}
	}

}


Image to_Image(cv::Mat cv_image) {

	Image image;
	if (cv_image.rows != cv_image.cols) {
		return image;
	}
	to_binary_image(cv_image);

	int interval = cv_image.rows / IMAGE_LENGTH;
	int offset = 0;// interval / 2;
	unsigned long long  mask = 0;
	int index = 0;
	int total_length = IMAGE_LENGTH * IMAGE_LENGTH;

	for (int h0 = 0; h0 < IMAGE_LENGTH; h0++) {
		const int h = h0 * interval + offset;
		for (int w0 = 0; w0 < IMAGE_LENGTH; w0++) {
			const int w = w0 * interval + offset;
			auto&  pixel = cv_image.at<cv::Vec3b>(h, w);
			const bool b = 255 == pixel[1];
			image.content[h0*IMAGE_LENGTH + w0] = b;// ? 1 : 0;
			unsigned long long ul_b = b ? 1 : 0;
			unsigned long long shift = (ul_b << (index++));
			if (index == total_length) {
				shift = 0;
			}
			mask += shift;
			//std::cout << b;
		}
	}
	//std::cout << std::endl;
	image.mask = mask;
	image.binary_image = cv_image;
	return image;

}



class Lane {

};



int recognize()
{
	using namespace cv;
	using namespace std;
	vector<Mat> image_vec;
	string dir = "./Google_Nav2/";

	vector<Image> image_vector;
	int bit_of_image = IMAGE_LENGTH * IMAGE_LENGTH;
	cv::Size new_size(132, 132);

	for (auto& filename : getAllImageFileNamesWithinFolder(dir)) {
		//cout << filename << endl;
		auto cv_img = cv::imread(dir + filename);
		cv::Mat resized_image;
		cv::resize(cv_img, resized_image, new_size, 0, 0, cv::INTER_NEAREST);
		cv_img = resized_image;

		Image img = to_Image(cv_img);
		cv::imwrite(filename, img.binary_image);

		unsigned long long mask = img.mask;
		std::bitset<64> binary(mask);
		cout << img.mask << " " << binary << " " << filename << endl;
		image_vector.push_back(img);

	}
	cout << endl;

	for (auto img1 : image_vector) {
		int sad_zero_count = 0;
		for (auto img2 : image_vector) {
			unsigned long sad = get_SAD(img1, img2);
			cout << std::hex << sad << " ";
			if (0 == sad) {
				sad_zero_count++;
			}
		}
		if (sad_zero_count != 1) {
			cout << "*";
		}
		cout << endl;
	}

	return 0;
}



#include <Vector.h>
#define String std::string

Vector<bool> getWhiteLane(int lanes, String str) {

	Vector<bool> result(lanes);
	for (int x = 0; x < lanes; x++) {
		result.push_back(false);
	}

	const char* buffer = str.c_str();
	char s[2] = ":";
	char *token = strtok((char*)buffer, s);

	for (int x = 0; token != NULL; x++, token = strtok(NULL, s)) {
		int num = atoi(token);
		if (num > lanes) {
			break;
		}
		else {
			result[num - 1] = true;
		}
	}
	return result;
}

int main() {
	if (false) {
		using namespace std;
		Vector<bool> vec = getWhiteLane(3, "3:");
		for (int x = 0; x < vec.size(); x++) {
			cout << vec[x] ? "1" : "0";

		}
		cout << endl;
		int a = 1;
	}
	if (false) {
		using namespace cv;
		using namespace std;
		vector<Mat> image_vec;
		string dir = "./Google_Nav/";


		Size size(IMAGE_LENGTH, IMAGE_LENGTH);
		Image img1 = to_Image(cv::imread("arrow0.png"));

		for (auto& filename : getAllImageFileNamesWithinFolder(dir)) {
			//cout << filename << endl;
			auto& cv_img = cv::imread(dir + filename);
			Image img2 = to_Image(cv_img);
			unsigned long sad = get_SAD(img1, img2);
			cout << filename << " sad: " << sad << endl;
		}
	}

	if (true) {
		recognize();
	}
}