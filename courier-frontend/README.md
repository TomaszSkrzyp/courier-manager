Courier Manager Frontend
This repository contains the user interface for our courier management system.
Use the following instructions to set up the project and make changes to the code.

Project Setup
To run the application on your local machine, follow these steps in order:
Install Libraries: Open your terminal and run npm install. This downloads the necessary files to run the project.
Start Development Mode: Run npm run dev. This will start the local server.
Access the Site: Open the URL provided in the terminal, which is usually http://localhost:5173.

The terminal must remain open while you are working. If the terminal is closed, the website will stop responding.

How to Edit the Project
We are using a single-page setup to keep things organized. You can find the main file at this path:
src/routes/+page.svelte

This file is the only one you need to modify for the user interface. It is divided into three sections:
Logic (Script): Located at the top between <script> tags. This is where we define variables and handle data.
Content (HTML): Located in the middle. This is where you change text, add buttons, or update the layout.
Design (Style): Located at the bottom between <style> tags. This is where you change colors and spacing.

Saving the file will automatically update the website in your browser. You do not need to refresh the page manually.