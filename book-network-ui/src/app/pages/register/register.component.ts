import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RegistrationsRequest } from 'src/app/services/models';
import { AuthenticationService } from 'src/app/services/services';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  constructor(
    private router: Router,
    private authService: AuthenticationService
  ) {}

  login() {
    this.router.navigate(['login']);
  }

  register() {
    this.errorMsg = [];
    this.authService
      .register({
        body: this.registerRequest,
      })
      .subscribe({
        next: () => {
          this.router.navigate(['activate-account']);
        },
        error: (err) => {
          console.log(err.error); // Log error response for debugging
          if (
            err.error?.validationErrors &&
            Array.isArray(err.error.validationErrors)
          ) {
            this.errorMsg = err.error.validationErrors; // Directly assign the validation errors
          } else if (err.error?.errorMsg) {
            this.errorMsg.push(err.error.error); // In case there's a generic error message
          } else {
            this.errorMsg.push('An unknown error occurred'); // Fallback error
          }
        },
      });
  }

  registerRequest: RegistrationsRequest = {
    email: '',
    firstname: '',
    lastname: '',
    password: '',
  };

  errorMsg: Array<string> = [];
}
